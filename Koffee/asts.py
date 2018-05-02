#-*- coding: utf-8 -*-
"""
ASTs, or abstract syntax trees are standard objects to help
write parsers for a language specified as a BNF grammar.
We define several useful ASTs to write our parser in the file *asts.py*.

.. module::ast
   :synopsis: abstract syntax trees for the parser.
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>
"""
from asttypes import *

def is_ast(cand):
    '''
    Determines whether an object is a defined AST

    Args:
    cand (object) : candidate AST

    Returns:
    result (bool) : whether candidate is an AST in our asttypes.

    '''
    try:
        try_type = cand.get_type()
        return True
    except:
        return False


class PgmAst(object):
    '''
    This is the AST of a general *Koord* program

    '''
    def __init__(self, name, flags=[]):
        '''
        '''
        self.name = name
        self.flags = flags+['javadef', 'default']

    def get_name(self):
        '''
        Get method for name.
        '''
        return self.name

    def get_type(self):
        '''
        This function is used to get the AST type to generate
        the java code.
        '''
        return PGMTYPE

    def get_flags(self):
        '''
        Get method for the program flags for various code generation requirements.
        '''
        return self.flags


    def add_flag(self, flag):
        '''
        Method to add a flag to the existing set of flags
        '''
        self.flags.append(flag)

    def has_flag(self, flag):
        '''
        Method to check whether the set of flags contains a given flag
        '''
        return flag in self.flags
