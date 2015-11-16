grammar disindent;

tokens { INDENT, DEDENT }

// Lexer taken from https://github.com/antlr/grammars-v4

@lexer::members {
  private java.util.LinkedList<Token> tokens = new java.util.LinkedList<>();
  private java.util.Stack<Integer> indents = new java.util.Stack<>();
  private int opened = 0;
  private Token lastToken = null;
  
  @Override
  public void emit(Token t) {
    super.setToken(t);
    tokens.offer(t);
  }

  @Override
  public Token nextToken() {
    if (_input.LA(1) == EOF && !this.indents.isEmpty()) {
      for (int i = tokens.size() - 1; i >= 0; i--) {
        if (tokens.get(i).getType() == EOF) {
          tokens.remove(i);
        }
      }

      this.emit(commonToken(disindentParser.NEWLINE, "\n"));

      while (!indents.isEmpty()) {
        this.emit(createDedent());
        indents.pop();
      }

      this.emit(commonToken(disindentParser.EOF, "<EOF>"));
    }

    Token next = super.nextToken();

    if (next.getChannel() == Token.DEFAULT_CHANNEL) {
      this.lastToken = next;
    }

    return tokens.isEmpty() ? next : tokens.poll();
  }

  private Token createDedent() {
    CommonToken dedent = commonToken(disindentParser.DEDENT, "");
    dedent.setLine(this.lastToken.getLine());
    return dedent;
  }

  private CommonToken commonToken(int type, String text) {
    int stop = this.getCharIndex() - 1;
    int start = text.isEmpty() ? stop : stop - text.length() + 1;
    return new CommonToken(this._tokenFactorySourcePair, type, DEFAULT_TOKEN_CHANNEL, start, stop);
  }

  static int getIndentationCount(String spaces) {
    int count = 0;
    for (char ch : spaces.toCharArray()) {
      switch (ch) {
        case '\t':
          count += 8 - (count % 8);
          break;
        default:
          // A normal space char.
          count++;
      }
    }

    return count;
  }

  boolean atStartOfInput() {
    return super.getCharPositionInLine() == 0 && super.getLine() == 1;
  }
}

program:
	(package_declaration NEWLINE)?
	(native_declaration NEWLINE)?
	(using_declaration | NEWLINE)*
	(function NEWLINE* | typedef NEWLINE* | nativeImport NEWLINE*)+
	EOF
	;
	
package_declaration:
	'in' javaName
	;
	
native_declaration:
	'native package' javaName
	;
	
javaName:
	(identifier '.')* identifier
	;

using_declaration:
	using_module | using_functions
	;
	
using_module:
	'use' fqNameImport NEWLINE
	;
	
fqModuleName:
	(identifier '.')* module_identifier
	;
	
fqNoModule:
	fqModuleName '.' identifier
	;
	
fqNameImport:
	fqModuleName | fqNoModule 
	;
	
fqName:
	fqNoModule | identifier
	;
	
using_functions:
	multiline_using | singleline_using 
	;
	
singleline_using:
	'from' fqModuleName 'use' uses 
	; 
	
multiline_using:
	'from' fqModuleName 'use' multiline_uses
	;
	
multiline_uses:
	NEWLINE INDENT uses+ DEDENT
	;

uses:
	(identifier ',')* identifier NEWLINE
	;
	
nativeImport:
	type 'native' identifier func_arguments NEWLINE
	;
	
function:
	header block
	;
	
header:
	type identifier func_arguments
	;

func_arguments:
	'(' parameters? ')'
	;
	
parameters:
	(parameter ',')* parameter
	;
	
parameter:
	identifier 'is' type
	;
	
block:
	NEWLINE INDENT operation+ DEDENT
	;
		
operation:
	  'call' head 'with' arguments
	| atom_operation
	| complex_operation
	;
	
complex_operation:
	if_operation
	;
	
if_operation:
	'if' atom NEWLINE INDENT operation operation? DEDENT
	;
	
operation_nonl:
	  'call' head 'with' arguments
	| atom
	| complex_operation
	;
	
atom_operation:
	atom NEWLINE
	;
	
arguments:
	NEWLINE INDENT operation+ DEDENT
	;
	
simple_op:
	'call' head 'with' '(' simple_arguments? ')'
	;
	
head:
	fqName | mathop
	;
	
mathop:
	'+' | '-' | '*' | '/'
	;
	
simple_arguments:
	(atom ',')* atom
	;

atom:
      simple_op
	| accessor
	| constArg
	| constList
	| cast
	;
	
cast:
	'use' '(' operation_nonl ')' 'as' type
	;
	
accessor:
	dottedName
	;
	
dottedName:
	(identifier '.')* identifier
	;
	
constList:
	type '[' atoms? ']'
	;
	
atoms:
	(atom ',')* atom
	;
	
constArg:
	IntegerConstant
	| LongConstant
	| DoubleConstant
	| FloatConstant
	| String
	| make
	| clone
	| 'none'
	| 'true'
	| 'false'
	| funcptr	
;

funcptr:
	'&' funcdesignator
	;
	
funcdesignator:
	fqName
	;
	
make:
	('make' fqName 'with' '(' assignments? ')') |
	('make' fqName 'with' INDENT (assignments NEWLINE)* assignments DEDENT)
	;

clone:
	('clone' fqName '<' atom '>' 'with' '(' assignments? ')') |
	('clone' fqName '<' atom '>' 'with' INDENT (assignments NEWLINE)* assignments DEDENT)
	;
	
assignments:
	(assignment ',')* assignment
	;
	
assignment:
	identifier 'is' atom
	;
	
type:
	typepart list*
	;
	
list:
	('[]')
	;
	
typepart:
	  'bool' 
	| 'byte' 
	| 'short' 
	| 'int' 
	| 'long' 
	| 'double' 
	| 'float' 
	| 'string' 
	| 'any' 
	| 'function' 
	| fqName
	;
	
typedef:
	typedef_header typedef_body
	;
	
typedef_header:
	'define' identifier NEWLINE
	;
	
typedef_body:
	INDENT field_declaration* DEDENT
	;
	
field_declaration:
	identifier 'as' type ('with' atom)? NEWLINE
	;
	
module_identifier:
	MODULE_IDENTIFIER
	;
	
identifier:
	IDENTIFIER
	;
	
String:
	'\'' SCharSequence? '\''
    ;
    
fragment SCharSequence:   
	SChar+
    ;
    
fragment SChar:
	   ~['\\\r\n]
    |   EscapeSequence
    ;
    
fragment EscapeSequence:
    SimpleEscapeSequence
    ;
    
fragment SimpleEscapeSequence:   
	'\\' ['nrt\\]
    ;
  
MODULE_IDENTIFIER
: [A-Z] ([a-z] | [0-9] | '_' | [A-Z])*
;

IDENTIFIER
 : [a-z] ([a-z] | [0-9] | '_' | [A-Z])*
 ;

fragment Digit:
   [0-9]
    ;

IntegerConstant:
	    DecimalConstant
    |   OctalConstant
    |   HexadecimalConstant
    |	BinaryConstant
    ;

LongConstant:
	    DecimalConstant 'l'
    |   OctalConstant 'l'
    |   HexadecimalConstant 'l'
    |	BinaryConstant 'l'
    ;

fragment BinaryConstant:
	'0' [bB] [0-1]+
	;

fragment DecimalConstant:
	   NonzeroDigit Digit*
    ;

fragment OctalConstant:
	'0' OctalDigit*
    ;

fragment HexadecimalConstant:
	HexadecimalPrefix HexadecimalDigit+
    ;

fragment HexadecimalPrefix:
   '0' [xX]
    ;

fragment NonzeroDigit:   
	[1-9]
    ;

fragment OctalDigit:
   [0-7]
   ;

fragment HexadecimalDigit:
   [0-9a-fA-F]
   ;

DoubleConstant
    :   DecimalFloatingConstant 
    |   HexadecimalFloatingConstant
    ;

FloatConstant
    :   DecimalFloatingConstant 'f'
    |   HexadecimalFloatingConstant 'f'
    ;

fragment
DecimalFloatingConstant
    :   FractionalConstant ExponentPart? 
    |   DigitSequence ExponentPart
    ;

fragment
HexadecimalFloatingConstant
    :   HexadecimalPrefix HexadecimalFractionalConstant BinaryExponentPart
    |   HexadecimalPrefix HexadecimalDigitSequence BinaryExponentPart
    ;

fragment
FractionalConstant
    :   DigitSequence? '.' DigitSequence
    |   DigitSequence '.'
    ;

fragment
ExponentPart
    :   'e' Sign? DigitSequence
    |   'E' Sign? DigitSequence
    ;

fragment
Sign
    :   '+' | '-'
    ;

fragment
DigitSequence
    :   Digit+
    ;

fragment
HexadecimalFractionalConstant
    :   HexadecimalDigitSequence? '.' HexadecimalDigitSequence
    |   HexadecimalDigitSequence '.'
    ;

fragment
BinaryExponentPart
    :   'p' Sign? DigitSequence
    |   'P' Sign? DigitSequence
    ;

fragment
HexadecimalDigitSequence
    :   HexadecimalDigit+
    ;

NEWLINE
 : ( {atStartOfInput()}?   SPACES
   | ( '\r'? '\n' | '\r' ) SPACES?
   )
   {
     String newLine = getText().replaceAll("[^\r\n]+", "");
     String spaces = getText().replaceAll("[\r\n]+", "");
     int next = _input.LA(1);

     if (opened > 0 || next == '\r' || next == '\n' || next == '#') {
       // If we're inside a list or on a blank line, ignore all indents, 
       // dedents and line breaks.
       skip();
     }
     else {
       emit(commonToken(NEWLINE, newLine));

       int indent = getIndentationCount(spaces);
       int previous = indents.isEmpty() ? 0 : indents.peek();

       if (indent == previous) {
         // skip indents of the same size as the present indent-size
         skip();
       }
       else if (indent > previous) {
         indents.push(indent);
         emit(commonToken(disindentParser.INDENT, spaces));
       }
       else {
         // Possibly emit more than 1 DEDENT token.
         while(!indents.isEmpty() && indents.peek() > indent) {
           this.emit(createDedent());
           indents.pop();
         }
       }
     }
   }
;

SKIP
 : ( SPACES | LINE_JOINING ) -> skip
 ;
 
 fragment SPACES
 : [ \t]+
 ;

fragment LINE_JOINING
 : '\\' SPACES? ( '\r'? '\n' | '\r' )
 ;
 
LineComment:
	'%' ~[\r\n]*
	    -> skip
	;