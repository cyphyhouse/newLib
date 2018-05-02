#-*- coding: utf-8 -*-
"""
*Koord* requires the following tokens to be parsed.
The developer needs to add tokens to *scanner.py* to add more syntax to *Koord*.

*reserved keywords and tokens*

1. *separators* : module, actuators, sensors, allwrite, allread, local, init
2. *defining keywords* : def, type, fun
3. *data types* : int, float, bool, pos
4. *control flow* : if, else, atomic, pre, eff
5. *constants* : true, false, pid, numAgents
6. *delimiters* : semicolon, colon, comma, parentheses, braces, brackets,angles
7. *boolean operators* : and, or , not
8. *identifiers* : lowercase\_id, uppercase\_id
9. *numerals* : int_num, float_num
10. *arithmetic operators* : +, -, \*, /
11. *relational operators* : <, >, ==, >=, <=, !=
12. *assignment* : =
13. *spacing* : whitespace, newline
14. *indentation* : indent, dedent

.. module::scanner
   :synopsis: Tokens.
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>

"""

from ply import *
#reserved keywords

RESERVED = {'agent': 'AGENT', 'module':'MODULE',
            'def' : 'DEF', 'type' : 'TYPE', 'fun' : 'FUN',
            'actuators':'ACTUATORS', 'sensors':'SENSORS',
            'allwrite':'ALLWRITE', 'allread':'ALLREAD', 'local':'LOCAL',
            'list':'LIST', 'map':'MAP', 'queue':'QUEUE',
            'init':'INIT', 'int':'INT', 'float':'FLOAT', 'bool':'BOOL',
            'pos':'POS', 'if':'IF', 'else':'ELSE', 'atomic':'ATOMIC',
            'pre':'PRE', 'eff':'EFF', 'true':'TRUE', 'false':'FALSE',
            'pid':'PID', 'numAgents':'NUMAGENTS'
           }

#additional required tokens

tokens = ['COLON', 'COMMA', 'SEMICOLON',
          'LPAR', 'RPAR', 'LBRACE', 'RBRACE', 'LCURLY', 'RCURLY', 'LANGLE', 'RANGLE',
          'AND', 'OR', 'NOT',
          'LID', 'CID',
          'INUM', 'FNUM',
          'PLUS', 'MINUS', 'TIMES', 'BY',
          'LT', 'GT', 'EQ', 'GEQ', 'LEQ', 'NEQ',
          'ASGN',
          'WS', 'NL',
          'INDENT', 'DEDENT',
         ] + list(RESERVED.values())

#decimal numbers
def t_FNUM(t):
    r'[-]?[0-9]+([.][0-9]+)?'
    t.value = float(t.value)
    return t

#integer numbers
def t_INUM(t):
    r'[-]?[0-9]+'
    t.value = int(t.value)
    return t


#delimiters
t_COLON = r':'
t_SEMICOLON = r';'
t_COMMA = r','


#arithmetic operators
t_PLUS = r'\+'
t_MINUS = r'-'
t_TIMES = r'\*'
t_BY = r'/'


#relational operators
t_GT = r'>'
t_LT = r'<'
t_GEQ = r'>='
t_LEQ = r'<='
t_EQ = r'=='
t_NEQ = r'\!='
t_AND = r'\&\&'
t_OR = r'\|\|'
t_NOT = r'\!'
#assignment
t_ASGN = r'='

#bracketing
t_LBRACE = r'\['
t_RBRACE = r'\]'
t_LCURLY = r'\{'
t_RCURLY = r'\}'
t_LANGLE = 'r\<'
t_RANGLE = 'r\>'

#capitalized identifiers
def t_CID(t):
    r'[A-Z][a-zA-Z0-9\.]*'
    t.type = RESERVED.get(t.value, 'CID')
    return t

#lowercase identifiers
def t_LID(t):
    r'[a-z][a-zA-Z0-9\.]*'
    t.type = RESERVED.get(t.value, 'LID')
    return t

#comments, start with a #
def t_comment(t):
    r"[ ]*\#[^\n]*"
    pass

#whitespace, ignore if not at start of line or if within a paranthesis
def t_WS(t):
    r' [ ]+ '
    if t.lexer.paren_count == 0 and t.lexer.line_start:
        return t

#newline, ignore if within paranthesis
def t_newline(t):
    r'\n+'
    t.lexer.lineno += len(t.value)
    t.type = 'NL'
    if t.lexer.paren_count == 0:
        return t

#left parenthesis , increment counter
def t_LPAR(t):
    r'\('
    t.lexer.paren_count += 1
    return t

#right paranthesis
#TODO: check for underflow in parser or a separate checker.
def t_RPAR(t):
    r'\)'
    t.lexer.paren_count -= 1
    return t

#unrecognized
def t_error(t):
    print("Skipping", repr(t.value[0]))
    t.lexer.skip(1)
    raise SyntaxError("Unknown symbol %r" %(t.value[0]))

#auxiliary function for synthesizing tokens
def _new_token(type, lineno):
    """
    auxiliary function for synthesizing tokens. We add a few attributes to bookkeep.
    1. type : token type.
    2. value : token value.
    3. lineno : line number of token.
    4. lexpos : column of token.
    """
    tok = lex.LexToken()
    tok.type = type
    tok.value = None
    tok.lineno = lineno
    tok.lexpos = 0
    return tok

#generate dedent token
def DEDENT(lineno):
    """
    generate dedent token
    """
    return _new_token("DEDENT", lineno)

#generate indent token
def INDENT(lineno):
    """
    generate indent token
    """
    return _new_token("INDENT", lineno)

