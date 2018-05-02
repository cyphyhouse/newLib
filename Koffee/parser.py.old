from scanner import * 
from ast import * 
global wnum
wnum = 0


def p_program(p):
    '''program : agent modules awdecls ardecls locdecls stagedecl init events Numdecl'''
    p[0] = pgmAst(p[1],p[2],p[6],p[3],p[4],p[5],p[7],p[8])
def p_Numdecl(p):
    '''Numdecl : NUM INUM NL'''
    pass

def p_stagedecl(p):
    '''stagedecl : DEF STAGE LCURLY stagelist RCURLY NL
                 | empty  
    '''
    if len(p) > 2: 
       p[0] = (p[4])
    else :
       p[0] = None

def p_stagelist(p):
    '''stagelist : LID COMMA stagelist
                 | LID 
    '''
    sl = [p[1]]
    if len(p) > 2:
      sl += p[3]
    p[0] = sl

def p_agent(p):
    '''agent : AGENT CID NL'''
    p[0] = p[2]


def p_modules(p):
    '''modules : module modules 
               | empty
    '''
    if len(p) > 2:
       p[0] = [p[1]]+p[2]
    else:
       p[0] = []
    
 
def p_module(p):
    '''module : USING MODULE CID COLON NL INDENT actuatordecls sensordecls DEDENT'''
    p[0] = p[3]


def p_actuatordecls(p):
    '''actuatordecls : ACTUATORS COLON NL INDENT decls DEDENT
                     | empty
    '''
    if len(p) >2 :
      p[0] = p[5]
    else:
      p[0] = p[1]

def p_sensordecls(p):
    '''sensordecls : SENSORS COLON NL INDENT decls DEDENT
                   | empty 
    '''
    if len(p) >2 :
      p[0] = p[5]
    else:
      p[0] = p[1]

def p_awdecls(p):
    '''awdecls : ALLWRITE COLON NL INDENT decls DEDENT
               | empty
    ''' 
    if len(p) > 2:
       p[0] = p[5]
    else:
       p[0] = None
 
def p_ardecls(p):
    '''ardecls : ALLREAD COLON NL INDENT rvdecls DEDENT
               | empty
    ''' 
    if len(p) > 2:
       p[0] = p[5]
    else:
       p[0] = None

def p_locdecls(p):
    '''locdecls : LOCAL COLON NL INDENT decls DEDENT
                | empty 
    '''
    if len(p) > 2:
       p[0] = p[5]
    else:
       p[0] = None

def p_decls(p):
    '''decls : decl decls 
            | empty
    '''
    dlist = []
    if len(p) == 3:
      dlist.append(p[1])
      dlist+= p[2]
    p[0] = dlist

def p_decl(p):
    '''decl : type varname ASGN exp NL
            |  type varname NL 
    '''
    if len(p) == 4:
      p[0] = (declAst(p[1],p[2]))
    else:
      p[0] = (declAst(p[1],p[2],p[4]))

def p_rvdecls(p):
    '''rvdecls : rvdecl rvdecls 
               | empty
    '''
    dlist = []
    if len(p) == 3:
      dlist.append(p[1])
      dlist+= p[2]
    p[0] = dlist
    

def p_rvdecl(p) :
    '''rvdecl : type varname LBRACE owner RBRACE NL 
    '''
    p[0] = (rvdeclAst(p[1],p[2],p[4]))
def p_owner(p) : 
      '''owner : TIMES 
               | INUM'''
      p[0] = p[1]

def p_funccall(p):
    '''funccall : varname LPAR args RPAR
    '''
    p[0] = funcAst(p[1],p[3])

def p_args(p):
    '''args : neargs 
            | noargs
    '''
    p[0] = p[1]

def p_noargs(p):
    '''noargs : empty'''
    p[0] = []

def p_neargs(p):
    '''neargs : exp 
              | exp COMMA neargs
    '''
    dlist = [p[1]]
    if len(p) > 2:
      dlist += p[3]
    p[0] = dlist

       
def p_type(p):
    '''type : INT
            | STRING 
            | FLOAT
            | IPOS
            | BOOLEAN
            | INPUTMAP 
    '''
    #print(p[0])
    p[0] = p[1]

def p_init(p):
    '''init : INIT COLON NL INDENT stmts DEDENT
            | empty
    '''
    if len(p) >2 :
       p[0] = initAst(p[5])
    else :
       p[0] = None
def p_events(p):
    '''events : event events
              | event '''
    dlist = [p[1]]
    if len(p) > 2:
       dlist+= p[2]
    p[0] = dlist

def p_event(p):
    '''event : LID COLON NL INDENT PRE COLON cond NL effblock DEDENT
    '''
    p[0] = eventAst(p[1],p[7],p[9])
        

def p_effblock(p):
    '''effblock : EFF COLON NL INDENT stmts DEDENT 
                | EFF COLON stmt 
    '''
    if len(p) > 4:
       p[0] = p[5]
    else:
       p[0] = p[3]


def p_cond(p):
    '''cond :  LPAR cond AND cond RPAR 
            | LPAR cond OR cond RPAR
            | LPAR cond op cond RPAR
            | LPAR NOT cond RPAR
            | exp
    '''
    if len(p) == 6:
       p[0] = condAst(p[2],p[4],p[3])
    elif len(p) == 5:
       p[0] = condAst(p[3],None,p[2])
    else:
       p[0] = condAst(p[1])
def p_stmts(p):
    '''stmts : stmt stmts
             | stmt'''
    dlist = [p[1]]
    if len(p) == 3:
      dlist+= p[2]
    p[0] = dlist
       
def p_stmt(p):
    '''stmt : asgn 
            | wptstmt
            | exit 
            | funccall NL
            | modulefunccall NL
            | ATOMIC COLON NL INDENT stmts DEDENT 
            | IF cond COLON NL INDENT stmts DEDENT elseblock
    '''
    if len(p) is 2:
       p[0] = p[1]
    elif len(p) is 3:
       p[0] = mkStmt(p[1])
    elif len(p) == 7:
       global wnum
       p[0] = atomicAst(wnum,p[5])
       wnum += 1
    else:
       p[0] = iteAst(p[2],p[6],p[8])

def p_exit(p): 
    '''exit : EXIT NL'''
    p[0] = exitAst()
def p_modulefunccall(p):
    '''modulefunccall : CID LPAR args RPAR '''
    p[0]= mfast(p[1],p[3]) 

def p_elseblock(p):
    '''elseblock : ELSE COLON NL INDENT stmts DEDENT
                 | empty
    '''
    if len(p) > 2:
       p[0] = p[5]
    else: 
       p[0] = None

def p_asgn(p):
    '''asgn : varname ASGN exp NL
            | stagechange 
    '''
    if len(p) is 2 :
       p[0] = p[1]
    else :
       p[0] = (asgnAst(p[1],p[3]))
    
def p_stagechange(p):
    '''stagechange : STAGE ASGN varname NL
    '''
    p[0] = stageAsgnAst(p[3]) 
def p_wptstmt(p):
    '''wptstmt : varname ASGN GETINPUT LPAR RPAR NL'''
    pass

precedence = (('left','PLUS','MINUS'),
              ('left','TIMES','BY'))

def p_exp(p):
    '''exp : bracketexp 
           | exp PLUS exp 
           | exp TIMES exp
           | exp MINUS exp 
           | exp BY exp
           | varname 
           | STAGE 
           | bval
           | pid
           | LID LBRACE exp RBRACE 
           | num
           | null
           | funccall
           | modulefunccall
    '''
    if len(p) == 2:
       p[0] = p[1]
    elif len(p) == 4:
       p[0]= exprAst(arithtype,p[1],p[3],p[2])
    else:
       p[0]=rvAst(p[1],p[3])

def p_bracketexp(p):
    '''bracketexp : LPAR exp RPAR '''
    p[0] = p[2]

def p_bval(p):
    '''bval : TRUE 
           | FALSE
    '''
    p[0] = exprAst(bvaltype,p[1])

def p_num(p):
    '''num : INUM 
           | FNUM
    '''
    p[0] = exprAst(numtype,p[1])

def p_pid(p):
    '''pid : PID
    '''
    p[0] = exprAst(restype,p[1])

def p_varname(p):
    '''varname : LID
    '''
    p[0] = exprAst(vartype,p[1])

def p_null(p):
    '''null : NULL 
    '''     
    p[0] = exprAst(nulltype,p[1])

def p_op(p):
    '''op : EQ 
          | NEQ 
          | GEQ
          | LEQ
          | GT
          | LT
    '''
    p[0] = p[1]
 
def p_empty(p):
    '''empty :'''
    pass

def p_error(p):
    print("syntax error in input on line ",p.lineno,p.type)



class myparser(object):
    def __init__(self,lexer=None):
        self.lexer = IndentLexer()
        self.parser = yacc.yacc()


    def parse(self,code):
        self.lexer.input(code)
        result = self.parser.parse(lexer=self.lexer)
        return result

def mkStmt(ast):
    bs = ast
    bs.isstmt = True
    return bs  
