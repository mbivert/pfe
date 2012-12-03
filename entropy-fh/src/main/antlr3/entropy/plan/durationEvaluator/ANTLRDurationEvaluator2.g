//
// Copyright (c) 2009 Ecole des Mines de Nantes.
// 
//  This file is part of Entropy.
// 
//  Entropy is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
// 
//  Entropy is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with Entropy.  If not, see <http://www.gnu.org/licenses/>.
//
grammar ANTLRDurationEvaluator2;

options {
	language = Java;
	output = AST;	
}

@lexer::header {
package entropy.plan.durationEvaluator;
}
@parser::header {
package entropy.plan.durationEvaluator;
}

PLUS 	:	'+';
MINUS	:	'-';
MULTIPLY:	'*';
DIV	:	'/';
MOD	:	'%';
POW	:	'^';
LPARA:	'(';
RPARA:	')';

fragment Letter: ('a'..'z' | 'A'..'Z');
VAR: Letter (Letter|'#' Letter | '_' Letter)*;
INT :	'0'..'9'+;

FLOAT  :   ('0'..'9')+ '.' ('0'..'9')+ ;

WS  :   (' ') {$channel=HIDDEN;};
    

evaluate: expression;
expression: mult ((PLUS|MINUS)^ mult)*;

powerable: term (POW^ term)?;	   
unary: MINUS powerable -> ^(MINUS powerable)
       |powerable;
mult: unary ((MULTIPLY|DIV|MOD)^ unary)*;

term:
	INT
	|FLOAT
	|VAR
	| LPARA expression RPARA -> expression
	;