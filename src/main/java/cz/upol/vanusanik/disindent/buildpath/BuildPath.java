package cz.upol.vanusanik.disindent.buildpath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Field_declarationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FqModuleNameContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Func_argumentsContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.FunctionContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.IdentifierContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ListContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.NativeImportContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.ProgramContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypeContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypedefContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.TypepartContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.UsesContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Using_declarationContext;
import main.antlr.cz.upol.vanusanik.disindent.parser.disindentParser.Using_functionsContext;

import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import cz.upol.vanusanik.disindent.buildpath.TypeRepresentation.SystemTypes;
import cz.upol.vanusanik.disindent.errors.BuildPathException;
import cz.upol.vanusanik.disindent.errors.BuildPathModuleNameIncorrectException;
import cz.upol.vanusanik.disindent.parser.DataSource;
import cz.upol.vanusanik.disindent.parser.ParserBuilder;
import cz.upol.vanusanik.disindent.parser.SimpleDataSource;
import cz.upol.vanusanik.disindent.runtime.DisindentClassLoader;
import cz.upol.vanusanik.disindent.utils.Utils;
import cz.upol.vanusanik.disindent.utils.Warner;

/**
 * Represents all modules as interfaces for lookup of methods/objects in
 * disindent.
 * 
 * @author Peter Vanusanik
 *
 */
public class BuildPath implements Serializable {
	private static final long serialVersionUID = 4991151370879114892L;
	/** BuildPath for the thread */
	private static ThreadLocal<BuildPath> instance = new ThreadLocal<BuildPath>();

	/**
	 * Returns build path. Build path is valid for single thread only (other
	 * threads either have to manually set it up if needed or have their own
	 * build paths).
	 * 
	 * @return
	 */
	public static BuildPath getBuildPath() {
		if (instance.get() == null)
			synchronized (instance) {
				if (instance.get() == null)
					createForThread();
			}
		return instance.get();
	}

	/**
	 * Instantiate new build path and store it for the thread.
	 */
	private static void createForThread() {
		instance.set(new BuildPath());
	}

	/**
	 * Sets the build path of this thread to the instance
	 * 
	 * @param bp
	 *            instance
	 */
	public static void setForThread(BuildPath bp) {
		instance.set(bp);
	}

	private BuildPath() {

	}

	private transient DisindentClassLoader dcl = new DisindentClassLoader(
			Runtime.class.getClassLoader());
	private Map<String, AvailableElement> availableElements = new TreeMap<String, AvailableElement>();
	Map<String, AvailableElement> bpElements = new TreeMap<String, AvailableElement>();
	private transient Set<Validator> validatorSet = new HashSet<Validator>();

	/**
	 * @return class loader for this thread
	 */
	public DisindentClassLoader getClassLoader() {
		return dcl;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		in.defaultReadObject();
		dcl = new DisindentClassLoader(Thread.currentThread()
				.getContextClassLoader());
	}

	/**
	 * Adds selected path to the disindent build path
	 * 
	 * @param path
	 *            file to be checked
	 */
	public synchronized void addPath(File path) {
		searchPath(path);
	}

	/**
	 * Recursively search directories and read files from them or
	 * subdirectories.
	 * 
	 * @param path
	 * @param packageName
	 */
	private void searchPath(File path) {
		File[] subelements = path.listFiles();
		for (File subelement : subelements) {
			if (subelement.isDirectory())
				searchPath(subelement);
			else {
				String name = subelement.getName();
				String ext = FilenameUtils.getExtension(name);

				if (ext.equals("din")) {
					addModuleToPath(subelement, name);
				}
			}
		}
	}

	/**
	 * Adds module and its typedefs to the build path
	 * 
	 * @param path
	 *            source file
	 * @param name
	 *            module name
	 */
	private void addModuleToPath(File path, String name) {
		name = FilenameUtils.getBaseName(StringUtils.capitalize(StringUtils
				.lowerCase(name)));
		if (!StringUtils
				.containsOnly(name,
						"abcdefghijklmnopqrstuvwxyz_0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")) {
			Warner.warn(new BuildPathModuleNameIncorrectException(
					"module name has to be valid identifier"));
			return;
		}

		ParserBuilder pb = new ParserBuilder();
		byte[] source;
		try {
			source = FileUtils.readFileToByteArray(path);
		} catch (Exception e) {
			Warner.warn(new BuildPathException(e));
			return;
		}

		pb.setInput(source);
		pb.setFilename(path.getName());
		disindentParser p = pb.build();

		try {
			addModuleToPath(p, path.getName(), name, source);
		} catch (Throwable t) {
			Warner.warn(new BuildPathException(t));
		}
	}

	/**
	 * Parses the module to get all the interface information for the build path
	 * 
	 * @param p
	 *            parsed source
	 * @param fname
	 *            original filename
	 * @param name
	 *            name of the source used as module name
	 * @param source
	 *            byte contents of the source file
	 */
	private void addModuleToPath(disindentParser p, String fname, String name,
			byte[] source) {
		ProgramContext pc = p.program();
		String packagePath = "";

		if (pc.package_declaration() != null)
			packagePath = pc.package_declaration().javaName().getText();

		Map<String, String> imports = parseImports(pc, name, packagePath);

		String slashPath = Utils.slashify(packagePath);
		AvailableElement ae = new AvailableElement();

		ae.modulePackage = packagePath;
		ae.source = source;
		ae.sourceName = fname;
		ae.elementDinName = name;
		ae.elementName = Utils.asModuledefJavaName(name);
		ae.slashPackage = slashPath;

		loadTypedefs(ae, pc, imports);
		loadFunctions(ae, pc, imports);

		availableElements.put(slashPath.equals("") ? ae.elementName : slashPath
				+ "/" + ae.elementName, ae);
		bpElements.put(
				packagePath.equals("") ? name : packagePath + "." + name, ae);
	}

	/**
	 * Parses source for imports resolving
	 * 
	 * @param pc
	 * @return
	 */
	private Map<String, String> parseImports(ProgramContext pc,
			String selfModule, String selfPackage) {
		Map<String, String> iMap = new HashMap<String, String>();

		for (Using_declarationContext ud : pc.using_declaration()) {
			if (ud.using_module() != null) {
				String fqName = ud.using_module().fqNameImport().getText();
				String[] split = Utils.splitByLastDot(fqName);
				iMap.put(split[1], fqName);
			} else {
				Using_functionsContext fc = ud.using_functions();
				List<UsesContext> usesList = Utils.searchForElementOfType(
						UsesContext.class, fc);
				FqModuleNameContext fmnc = Utils
						.searchForElementOfType(FqModuleNameContext.class, fc)
						.iterator().next();

				String moduleName = fmnc.getText();
				for (UsesContext usc : usesList) {
					for (IdentifierContext i : usc.identifier()) {
						iMap.put(i.getText(), moduleName);
					}
				}
			}
		}

		for (TypedefContext tc : pc.typedef()) {
			String typedefName = tc.typedef_header().identifier().getText();
			String fqName = (selfPackage.equals("") ? selfModule : selfPackage
					+ "." + selfModule)
					+ "." + typedefName;
			iMap.put(typedefName, fqName);
		}

		return iMap;
	}

	/**
	 * Loads all typedefs to new available elements linked to provided available
	 * element
	 * 
	 * @param ae
	 *            module element to link new typedefs
	 * @param pc
	 *            parsed code
	 * @param imports
	 */
	private void loadTypedefs(AvailableElement ae, ProgramContext pc,
			Map<String, String> imports) {
		for (TypedefContext tc : pc.typedef()) {
			AvailableElement te = new AvailableElement();
			te.elementDinName = tc.typedef_header().identifier().getText();
			te.elementName = ae.elementName + "$"
					+ Utils.asTypedefJavaName(te.elementDinName);
			te.modulePackage = ae.modulePackage;
			te.slashPackage = ae.slashPackage;
			te.module = ae;
			if (ae.typedefs.contains(te.elementDinName))
				throw new BuildPathException("duplicate typedef name");
			ae.typedefs.add(te.elementDinName);

			loadTypedef(te, tc, imports);

			availableElements.put(ae.slashPackage.equals("") ? te.elementName
					: ae.slashPackage + "/" + te.elementName, ae);
			bpElements.put((ae.modulePackage.equals("") ? ae.elementDinName
					: ae.modulePackage + "." + ae.elementDinName)
					+ "."
					+ te.elementDinName, te);
		}
	}

	/**
	 * Parses the typedef definiton and extracts the type informations
	 * 
	 * @param te
	 *            typedef element
	 * @param tc
	 *            parsed typedef
	 * @param imports
	 *            list of imports
	 */
	private void loadTypedef(AvailableElement te, TypedefContext tc,
			Map<String, String> imports) {
		for (Field_declarationContext fc : tc.typedef_body()
				.field_declaration()) {
			String fieldName = fc.identifier().getText();
			TypeContext typec = fc.type();
			TypepartContext typepc = typec.typepart();

			String tt;
			if (typepc.fqName() != null)
				tt = typepc.fqName().getText();
			else
				tt = typepc.getText();

			TypeRepresentation type = asType(tt, typec, imports, te.module);

			te.fieldSignatures.addField(fieldName, type);
		}
	}

	/**
	 * Returns type as TypeRepresentation
	 * 
	 * @param tt
	 *            name of the type
	 * @param fc
	 *            type context
	 * @param imports
	 *            list of imports
	 * @param mae
	 *            module elemnt to be validated if type exists or not
	 * @return
	 */
	private TypeRepresentation asType(String tt, TypeContext fc,
			Map<String, String> imports, AvailableElement mae) {
		TypeRepresentation tr = null;

		tr = TypeRepresentation.asSimpleType(tt);
		if (tr == null) {
			String fqPath = imports.get(tt);
			if (fqPath == null)
				fqPath = tt;

			tr = new TypeRepresentation();
			tr.setType(SystemTypes.CUSTOM);
			tr.setFqTypeName(fqPath);
			validatorSet.add(new TypeValidator(mae, tr));
		}

		for (@SuppressWarnings("unused")
		ListContext lc : fc.list()) {
			TypeRepresentation oldr = tr;
			tr = new TypeRepresentation();
			tr.setType(SystemTypes.COMPLEX);
			tr.setSimpleType(oldr);
		}

		return tr;
	}

	/**
	 * Loads all function signatures from the module
	 * 
	 * @param ae
	 *            module element to store to
	 * @param pc
	 *            parsed code
	 * @param imports
	 */
	private void loadFunctions(AvailableElement ae, ProgramContext pc,
			Map<String, String> imports) {
		for (FunctionContext fc : pc.function()) {
			loadFunctionOrNative(fc, ae, imports);
		}
		for (NativeImportContext nic : pc.nativeImport()) {
			loadFunctionOrNative(nic, ae, imports);
		}
	}

	/**
	 * Loads function or native to the module
	 * 
	 * @param p
	 * @param ae
	 * @param imports
	 */
	private void loadFunctionOrNative(ParseTree p, AvailableElement ae,
			Map<String, String> imports) {
		TypeContext returnContext = Utils.searchForElementOfType(
				TypeContext.class, p).get(0);
		IdentifierContext name = Utils.searchForElementOfType(
				IdentifierContext.class, p).get(0);
		Func_argumentsContext args = Utils.searchForElementOfType(
				Func_argumentsContext.class, p).get(0);

		String baseName = name.getText();
		List<TypeContext> types = new ArrayList<TypeContext>();

		types.add(returnContext);
		for (TypeContext t : Utils.searchForElementOfType(TypeContext.class,
				args)) {
			types.add(t);
		}

		List<TypeRepresentation> trl = new ArrayList<TypeRepresentation>();
		for (TypeContext t : types) {
			TypepartContext typepc = t.typepart();

			String tt;
			if (typepc.fqName() != null)
				tt = typepc.fqName().getText();
			else
				tt = typepc.getText();

			trl.add(asType(tt, t, imports, ae));
		}

		ae.functionSignatures.addFunction(baseName, trl);
	}

	/**
	 * Returns data source of that java class (din element encoded in that
	 * class). If it is module class, module information is returned, if it is a
	 * typename within a module, module information of that typename is returned
	 * 
	 * @param name
	 *            java slashify version of class name
	 * @return
	 */
	public synchronized DataSource getClassSource(String name) {
		AvailableElement ae = availableElements.get(name);
		if (ae == null)
			return null;

		if (ae.module != null)
			ae = ae.module;

		if (!ae.valid)
			return null;

		SimpleDataSource ds = new SimpleDataSource();
		ds.filename = ae.sourceName;
		ds.is = new ByteArrayInputStream(ae.source);

		return ds;
	}

	/**
	 * Validates the build path and link all the unresolved imports.
	 */
	public void validate() {
		for (Validator v : validatorSet)
			v.validate(this);
	}

	public FunctionSignatures getSignatures(String packageName,
			String moduleName) {
		String fqName = packageName.equals("") ? moduleName : packageName + "."
				+ moduleName;
		AvailableElement ae = bpElements.get(fqName);
		if (ae != null)
			return ae.functionSignatures;
		return null;
	}

	public Set<String> getTypedefs(String packageName, String moduleName) {
		String fqName = packageName.equals("") ? moduleName : packageName + "."
				+ moduleName;
		AvailableElement ae = bpElements.get(fqName);
		if (ae != null)
			return ae.typedefs;
		return null;
	}

	public FieldSignatures getFieldSignatures(String fqTypeName) {
		AvailableElement ae = bpElements.get(fqTypeName);
		if (ae != null)
			return ae.fieldSignatures;
		return null;
	}

	/**
	 * Returns module as java class type
	 * 
	 * @param moduleName
	 * @return
	 */
	public String getClassPath(String moduleName) {
		AvailableElement ae = bpElements.get(moduleName);
		if (ae != null)
			return ae.slashPackage.equals("") ? ae.elementName
					: ae.slashPackage + "/" + ae.elementName;
		return null;
	}

	private final String uuid = UUID.randomUUID().toString();

	public String getUid() {
		return uuid;
	}

	public static boolean isEmpty() {
		return instance.get() == null;
	}

}
