#coding: utf-8 -*-
"""
This is a test file allowing the user to test the major portions of the code. 

.. module::test
   :synopsis: test for major portions. 
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>
"""


from ply import * 
from scanner import *
from indentlexer import * 

s = "test.krd"
LEXER.input(open(s).read())
while True :
   tok = LEXER.token()
   if not tok:
     break
   else: 
     print(tok)
