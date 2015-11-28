grammar disindent;

//compiler args compiler
compiler_args:
	compiler_arg* EOF
	;

compiler_arg:
	(identifier '=' const_arg) | identifier
	;

// program
program:
	( package_decl
	| native_decl
	| use_decl
	| include_decl
	| toplevel_form
	)*
EOF
;
	
toplevel_form:
	  native_type
	| func_decl
	| funn_decl
	| type_decl
	| define_compiler_constant
	| include_compiler_constants
	;

package_decl:
	'(' 'package' complex_identifier ')'
;

native_decl:
	'(' 'native' complex_identifier ')'
;

native_type:
	'(' 'native' 'type' identifier ')'
	; 

use_decl:
	'(' 'use' complex_identifier ')'
;

include_decl:
	'(' 'include' include_list 'from' complex_identifier ')'
;

include_list:
	'(' identifier+ ')'
;

func_decl:
	'(' 'def' fqtype '[' formal_params? ']' block ')'
;

funn_decl:
	'(' 'defn' fqtype '[' formal_params? ']' block ')'
;

formal_params:
	fqtype+
;

type_decl:
	'(' 'deft' identifier type_body+ ')'
;

type_body:
	fqtype
;

block:
	block_compiler_directive? expression*
;

complex_expression:
	  if_expression
	| for_expression
	| forlist_expression
	| lambda_expression
;

if_expression:
	'(' 'if' expression expression expression? ')'
;

for_expression:
	'(' 'for' '(' '(' identifier? fqtype ')' expression expression expression ')' block ')'
;

forlist_expression:
	'(' 'foreach' '(' '(' identifier? fqtype ')' expression ')' block ')'
;

lambda_expression:
	'(' 'fnc' '->' type '[' formal_params? ']' block ')'
;

expression:
	  complex_expression				#complexexpr
	| type_expression					#typeexpr
	| math_expression					#mathexpr
	| '(' expression+ ')'				#funcallexpr
	| expression ':' identifier   		#accessorexpr
	| var								#varexpr
	| const_arg							#constargexpr
	| make								#makeexpr
	| const_list 						#listexpr
	| compiler_expression				#compilerexpr
	| macro complex_expression			#complexexpr
	| macro type_expression				#typeexpr
	| macro math_expression				#mathexpr
	| macro '(' expression+ ')'			#funcallexpr
	| macro expression ':' identifier   #accessorexpr
	| macro var							#varexpr
	| macro const_arg					#constargexpr
	| macro make						#makeexpr
	| macro const_list 					#listexpr
	| macro compiler_expression			#compilerexpr
;

macro:
	'@'
	;
	
var:
	inhibited? complex_identifier
	;
	
inhibited:
	'~'
	;

type_expression:
	'(->' type expression ')'
;

math_expression:
	'(' mathop expression* ')'
;

mathop:
	'+' | '-' | '*' | '/' | compop
;

compop:
	'=' | '>' | '<' | '>=' | '<=' | '<>'
;

const_list:
	'(' '->' type '[' expression* ']' ')'
;

const_arg:
	  IntegerConstant
	| LongConstant
	| DoubleConstant
	| FloatConstant
	| String
	| 'none'
	| 'true'
	| 'false'
;

define_compiler_constant:
	'(' 'define-compiler-constant' identifier const_arg ')'
	;
	
include_compiler_constants:
	'(' 'constant' 'import' 'from' complex_identifier ')'
	;

directive_expression:
	const_arg | identifier
	;
	
cdirective_head:
	  'compiler-directive'
	| '#'
	;

// available compiler directives
	//
	//

block_compiler_directive:
	'(' cdirective_head directive_expression+ ')'
	;

compiler_expression:
	'(' cdirective_head directive_expression+ block ')'
	;

make:
	'(make' (type | '(' type expression ')' )
		make_body* ')'
;

make_body:
	'(' identifier expression ')'
;

fqtype:
	identifier '->' type
;

type:
	base_type multiplier*
;

multiplier:
	'[L]'
;

base_type:
	  simple_type					 
	| complex_identifier 
	| function_type		 
;

simple_type:
	  'bool'
	| 'byte'
	| 'short'
	| 'int' 
	| 'long'
	| 'float'
	| 'double'
	| 'string'
	| 'callable'
	;

function_type:
	'(' type '<-' type* ')'
;

complex_identifier:
	(identifier '::')* identifier
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

IDENTIFIER
 : ([a-z] | '_') ([a-z] | [0-9] | '_' | [A-Z])*
 ;

fragment Digit:
   [0-9]
    ;

IntegerConstant:
    	DecimalConstant 'I'?
    |   OctalConstant 'I'?
    |   HexadecimalConstant 'I'?
    |   BinaryConstant 'I'?
    ;

LongConstant:
    	DecimalConstant ('l' | 'L')
    |   OctalConstant ('l' | 'L')
    |   HexadecimalConstant ('l' | 'L')
    |   BinaryConstant ('l' | 'L')
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
    :   DecimalFloatingConstant 'D'? 
    |   HexadecimalFloatingConstant 'D'?
    ;

FloatConstant
    :   DecimalFloatingConstant ('f' | 'F')
    |   HexadecimalFloatingConstant ('f' | 'F')
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
 : ( '\r'? '\n' | '\r' ) SPACES? -> skip
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
