#-*- coding: utf-8 -*-
""" The file *codegen.py* includes the code generation functions
which takes in the abstract syntax trees and generates the corresponding
java code.

.. module::codegen
   :synopsis: Code generation functions.
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>
"""

import generic
from asts import is_ast
from asttypes import *
from ic import gen_ic
import mdecls

def get_package_decl(app_name):
    '''
    Function to generate package declaration from application name.
    '''
    return generic.add_nl(generic.mk_stmt("package " + app_name.lower()), 2)


def get_class_decl(app_name):
    '''
    Function to generate the Java class declaration.

    '''
    return "public class " + app_name.title() + "App extends LogicThread "

def get_class_method(app_name):
    '''
    class generation method with gvh input
    '''
    return "public " + app_name.title() + "(GlobalVarHolder gvh) "

def get_starl_code(app_code):
    '''
    function for wrapping the event code in the loop.
    '''
    return_code = "@Override\n"
    return_code += "public List<Object> callStarL() "
    app_code = "while(true) " + generic.mk_block(app_code, 1)
    return_code += generic.mk_block(app_code, 1)
    return return_code

def codegen(input_ast, tabs=0, wnum=0):
    '''
    The main code generation function. It takes as input an AST,
    and returns its corresponding java code. It is called recursively
    on the branches of the syntax tree.

    Args:
        input_ast (ast): if its not an AST, then return nothing, else generate code.
        tabs (int): indentation for generated java program.

    Returns:
        generated_code (str): java code as a string.

    '''
    generated_code = ""

    if not is_ast(input_ast):
        return generated_code

    inputast_type = input_ast.get_type()

    if inputast_type == PGMTYPE:

        app_name = input_ast.get_name()
        include_code = ""

        for flag in input_ast.get_flags():
            include_code += generic.mk_indent(gen_ic(flag), tabs)
        generated_code += get_package_decl(app_name)
        generated_code += include_code
        generated_code += get_class_decl(app_name)
        block_code = generic.add_nl(mdecls.mandatory_decls(input_ast, wnum), 2)
        block_code += get_class_method(app_name)
        init_code = mdecls.mandatory_inits(input_ast, wnum)
        block_code += generic.mk_block(init_code, tabs + 1)
        event_code = ""
        block_code += generic.mk_indent(get_starl_code(event_code), tabs)
        generated_code += generic.mk_block(block_code, tabs + 1)

    return generated_code
