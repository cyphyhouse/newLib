#-*- coding: utf-8 -*-
"""
Koord is designed to be an indentation based language
so we implemented a basic indentation lexer for it.
We used python-lex-yacc (ply) for this.


.. module::indentlexer
   :synopsis: indentation based lexical analyzer.
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>

"""
from ply import *
from scanner import *

#macros
NO_INDENT = 0
MAY_INDENT = 1
MUST_INDENT = 2


#implementing a white space filter which makes sure to ignore white spaces
#which are not at the start of a line
def ws_filter(lexer, tokens):
    """
    implementing a white space filter which makes sure to ignore
    white spaces which are not at the start of a line

    Args:
        lexer (lexer) : the lexer object.
        tokens (tokens) : tokens of the lexer.
    Returns:
        (none) : yields the set of tokens without irrelevant spaces

    """
    lexer.line_start = line_start = True
    indent = NO_INDENT
    for token in tokens:
        token.line_start = line_start
        #if a colon is seen, the next line may be indented if a complex statement,
        #not indented if simple statement
        if token.type == 'COLON':
            line_start = False
            indent = MAY_INDENT
            token.must_indent = False
        #if a new line is seen, if it saw a colon before,
        #then it must indent . otherwise ignore.
        elif token.type == "NL":
            line_start = True
            if indent == MAY_INDENT:
                indent = MUST_INDENT
            token.must_indent = False
        #white spaces must be at the start of a line.
        elif token.type == 'WS':
            assert token.line_start
            line_start = True
            token.must_indent = False
        else:
            token.must_indent = (indent == MUST_INDENT)
            line_start = False
            indent = NO_INDENT
        yield token
        lexer.line_start = line_start

#see gardensnake.py in the ply distribution for documentation on this. copied almost line by line.

def indent_filter(tokens):
    """
    implementing an indentation filter which adds correct indents and dedents

    Args:
        tokens (tokens) : tokens of the lexer.
    Returns:
        (none) : yields proper indentation, or raises indentation errors.

    """
    levels = [0]
    token = None
    depth = 0
    prev_ws = False

    for token in tokens:
        if token.type == 'WS':
            assert depth == 0

            depth = len(token.value)
            prev_ws = True

            continue

        elif token.type == 'NL':
            depth = 0
            if prev_ws or token.line_start:
                continue
            yield token
            continue

        prev_ws = False
        if token.must_indent:
            if depth <= levels[-1]:
                raise IndentationError("expected an indented block at line %r" %(token.lineno))
            levels.append(depth)
            yield INDENT(token.lineno)

        elif token.line_start:
            if depth == levels[-1]:
                pass
            elif depth > levels[-1]:
                raise IndentationError("indentation increase but not in new block")
            else:
                try:
                    i = levels.index(depth)
                except ValueError:
                    raise IndentationError("inconsistent indentation")
                for _ in range(i+1, len(levels)):
                    yield DEDENT(token.lineno)
                    levels.pop()
        yield token

    if len(levels) > 1:
        assert token is not None
        for _ in range(1, len(levels)):
            yield DEDENT(token.lineno)


#final filter. may need an endmarker.

def filter(lexer):
    """
    implementing an lexer filter which iteratively applies the filters,
    and yields the filtered tokens.
    See gardensnake in ply documentation for more, this is almost a carbon copy.

    Args:
        tokens (tokens) : tokens of the lexer.
    Returns:
        (none) : yields correct tokens.

    """
    token = None
    tokens = iter(lexer.token, None)
    tokens = ws_filter(lexer, tokens)
    for token in indent_filter(tokens):
        yield token

class IndentLexer(object):
    """
    The indent lexer class, which can create a lexer
    and return indented code in terms of tokens.
    """
    def __init__(self):
        self.lexer = lex.lex()
        self.token_stream = None


    def input(self, code):
        """
        function to take in code, and apply the filters.
        """
        self.lexer.paren_count = 0
        self.lexer.input(code)
        self.token_stream = filter(self.lexer)


    def token(self):
        """
        function to yield the next token.
        """
        try:
            #print(self.lexer.paren_count)
            return self.token_stream.next()
        except StopIteration:
            return None


LEXER = IndentLexer()
