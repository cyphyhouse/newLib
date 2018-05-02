#-*- coding: utf-8 -*-
""" The file *kparser.py* includes a parser for *Koord* programs
which enables us to generate Java code. We use the abstract syntax
tree module to construct the syntax tree from this parser.
The syntax of *Koord* is given as follows :

:pgm: **agnt**
:agnt: AGENT CID NL


The parser uses the indentation lexer to tokenize the code,
and passes the parsed result as an abstract syntax
tree to the compiler, which in turn generates java code.

.. module::kparser
   :synopsis: Parser.
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>
"""

from ply import *
from asts import *
from scanner import *
from indentlexer import *

def p_pgm(p):
    '''pgm : agnt modules'''
    p[0] = PgmAst(p[1], p[2][0])

def p_modules(p):
    '''modules : module modules
               | empty
    '''
    #module modules
    if len(p) > 2:
        #[modulenames],[associated declarations]
        p[0] = ([p[1][0]] + p[2][0], [p[1][1]] + p[2][1])
    #empty
    else:
        p[0] = ([], [])

def p_module(p):
    '''module : MODULE CID COLON NL INDENT adecls sdecls DEDENT'''
    #(modulename,list of actuator decls, list of sensordecls)
    p[0] = (p[2], p[6], p[7])

def p_adecls(p):
    '''adecls : ACTUATORS COLON NL INDENT decls DEDENT
                     | empty
    '''
    if len(p) > 2:
        p[0] = p[5]
    else:
        p[0] = []

def p_sdecls(p):
    '''sdecls : SENSORS COLON NL INDENT decls DEDENT
              | empty
    '''
    if len(p) > 2:
        p[0] = p[5]
    else:
        p[0] = []

def p_decls(p):
    '''decls : decl decls
             | empty
    '''
    #decl decls
    if len(p) > 2:
        p[0] = [p[1]]+ p[2]
    #decl
    else:
        p[0] = []

def p_decl(p):
    '''decl : type varname ASGN exp NL
            |  type varname NL
    '''
    if len(p) == 4:
        p[0] = []
    else:
        p[0] = []

def p_type(p):
    '''type : INT
            | FLOAT
            | POS
            | BOOL
    '''
    p[0] = []

def p_varname(p):
    '''varname : LID
    '''
    p[0] = []

def p_exp(p):
    '''exp : INUM
    '''
    p[0] = p[1]

def p_agnt(p):
    '''agnt : AGENT CID NL'''
    p[0] = p[2]

def p_empty(p):
    '''empty :'''
    pass

def p_error(p):
    '''to find line with error
    '''
    print("syntax error in input on line ", p.lineno, p.type)

class myparser(object):
    '''We create a parser class which can take in a different lexical analyzer as well
    '''
    def __init__(self, lexer=None):
        '''
        The basic parser class

        Args:
            lexer (lexer) : the lexer to tokenize the code.

        '''
        self.lexer = IndentLexer()
        self.parser = yacc.yacc()

    def parse(self, code):
        '''
        The function to take code as input, tokenize it using the lexer,
        and parse it.

        Args:
            code (str) : code to be parsed.

        Returns:
            result (pgmAst) : return the abstract syntax tree of the program

        '''
        self.lexer.input(code)
        result = self.parser.parse(lexer=self.lexer)
        return result

