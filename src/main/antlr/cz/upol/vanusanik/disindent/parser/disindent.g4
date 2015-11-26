grammar disindent;

program:
	( package_decl
	| native_decl
	| native_type
	| use_decl
	| include_decl
	| func_decl
	| funn_decl
	| type_decl
	)*
EOF
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
	'(use' complex_identifier ')'
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
	fqtype | '(' fqtype expression ')'
;

block:
	expression*
;

complex_expression:
	  if_expression
	| for_expression
	| lambda_expression
;

if_expression:
	'(' 'if' expression expression expression? ')'
;

for_expression:
	'(' 'for' '(' '(' identifier? fqtype ')' expression expression expression ')' block ')'
;

lambda_expression:
	'(' 'fnc' '->' type '[' formal_params? ']' block ')'
;

expression:
	  complex_expression			#complexexp
	| type_expression				#typeexp
	| math_expression				#mathexp
	| '(' expression+ ')'			#funcallexp
	| expression ':' identifier     #accessorexp
	| identifier					#varexp
	| const_arg						#constargexp
	| const_list 					#listexp
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
	| make
	| 'none'
	| 'true'
	| 'false'
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
	| constructor		 
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

constructor:
	'<' complex_identifier '>'
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
    |   BinaryConstant
    ;

LongConstant:
    	DecimalConstant 'l'
    |   OctalConstant 'l'
    |   HexadecimalConstant 'l'
    |   BinaryConstant 'l'
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
