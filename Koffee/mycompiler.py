#-*- coding: utf-8 -*-
""" The file *mycompiler.py* has a compiler which generates Java code
from *Koord* programs. We use the parser and the code generation modules
together to do this.

.. module::mycompiler
   :synopsis: Compiler.
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>
"""

import kparser
from codegen import codegen

class MyCompiler(object):
    '''
    This is the compiler object that is used to generate java code from *Koord*.
    '''

    def __init__(self):
        '''
        '''
        self.parser = kparser.myparser()

    def compile(self, filename):
        '''
        This function generates the abstract syntax tree from a *Koord* program.
        '''

        code = open(filename, "r").read()
        pgm = (self.parser.parse(code))
        return pgm

    def gen_java(self, filename):
        '''
        Function to call the code generation on the AST obtained after compiling.
        '''
        return codegen(self.compile(filename))

def krd_to_java(filename):
    '''
    Function to use the compiler object and generate java file
    '''
    pgm = MyCompiler().compile(filename)
    java_filename = pgm.get_name()+"App.java"
    java_file = open(java_filename, "w")
    code = MyCompiler().gen_java(filename)
    java_file.write(code)
    java_file.close()

krd_to_java("test.krd")
