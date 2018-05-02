#-*- coding: utf-8 -*-

""" We implemented some generic reusable functions to serve formatting needs.

.. module:: generic
   :synopsis: generic functions to help formatting
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>

"""
#add newlines to a statement
def add_nl(code, num=0):
    '''
    function to add newlines to a block of code.

    Args:
        code (str) : code to add new lines to.

    Returns:
        str : formatted code.
    '''
    return code + num * "\n"

#insert semicolon and space at the end of a statement.
def mk_stmt(stmt):
    """

    function to insert semicolon and space at the end of a statement

    Args:
        stmt (str) : statement to be processed.
    Returns:
        str : formatted statement

    """
    return stmt+";\n"

#create indentations for blocks of code.
def mk_indent(code, tabs):
    """
    function to create indentation for blocks of code

    Args:
        code (str) : Code to be indented.
        tabs (int) : Number of tabs to indent by.
    Returns:
        str : indented code

    """
    #indent every new line
    return_str = ""
    lines = code.split("\n")
    for line in lines:
        return_str += (tabs*"   ")+line+'\n'
    return return_str


#make an indented block of code
def mk_block(code, tabs):
    '''
    function to make a braces enclosed indented block of code.

    Args:
        code (str) : Code to be made into a block.
        tabs (int) : Number of tabs to indent by.
    Returns:
        str : code as a block

    '''
    return " {\n\n" + mk_indent(code, tabs) + "}\n"
