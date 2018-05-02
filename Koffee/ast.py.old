from symtab import * 
LOCAL = -1
MULTI_WRITER = -2
MULTI_READER = -3
CONTROLLER = -4

mftype = 'mf'
nulltype = 'nt'
restype = 'res'
exittype = 'exit'
stctype = 'stc'
vartype = 'var'
numtype = 'num'
bvaltype = 'bval'
arithtype = 'arith'
pgmtype = 'pgm'
decltype = 'decl'
rvdecltype = 'rvdecl'
inittype = 'init'
evnttype = 'evnt'
moduletype = 'mdl'
condtype = 'cond'
exprtype = 'expr'
functype = 'func' 
atomictype = 'atom'
asgntype = 'asgn'
mfasttype = 'mfast'
itetype = 'ite'
rvtype = 'rvtype' 

class pgmAst(list):
   def __init__(self,name,modules,stages,awdecls,ardecls,locdecls,init,events):
       self.name = name
       self.modules = modules
       self.stages = stages
       self.awdecls = awdecls
       self.ardecls = ardecls
       self.locdecls = locdecls
       self.init = init
       self.events = events

   def getflags(self):
        modules = []
        hasShared = False
        if self.modules == []:
            pass
        else :
            for module in self.modules: 
               modules.append(module.getName());
        if self.awdecls == []:
            pass
        else :
            hasShared = True 
        if self.ardecls == []:
            pass
        else :
            hasShared = True 
   def __repr__(self):
       return "sl" 
    
   def getflags(self):
        modules = []
        if self.modules == None :
            pass
        else :
            for module in self.modules: 
               modules.append(module.getName());
  
   def get_type(self):
       return pgmtype



class declAst(list):
    def __init__(self,dtype, varname, value= None ,scope=LOCAL,module = None):
        self.scope = scope
        self.dtype = dtype
        self.varname = varname
        self.value = value
        self.module = module

    def __repr__(self):
        dtype_str = str(self.dtype)+" "
        varname_str = str(self.varname)+" "
	value_str = ""
        if self.value is not None : 
            value_str += "= "+str(self.value)
        if self.module is not None:
            value_str += " "+str(self.module)
        return (dtype_str+varname_str+value_str)

    def get_scope(self):
        return self.scope

    def set_scope(self,scope):
        self.scope = scope

    def get_type(self):
        return decltype

class rvdeclAst(list):

    def __init__(self,dtype, varname, owner, value= None ,scope=LOCAL,module = None):
        self.scope = scope
        self.dtype = dtype
        self.varname = varname
        self.owner = owner
        self.value = value
        self.module = module

    def __repr__(self):
        dtype_str = str(self.dtype)+" "
        varname_str = str(self.varname)+" "
        owner_str = "["+str(self.owner)+"] "
	value_str = ""
        if self.value is not None : 
            value_str = "= "+str(self.value)
        return (dtype_str+varname_str+owner_str+value_str)

    def get_scope(self):
        return self.scope

    def set_scope(self,scope):
        self.scope = scope 

    def get_type(self):
        return rvdecltype

def mkEntry(decl):
    a = symEntry(decl.dtype,decl.varname,decl.scope)  
    a.module = decl.module
    return a 

def isAst(ast):
   try:
      t = ast.get_type()
      return True
   except:
      return False 


def getEntry(v,symtab):
  for entry in symtab[0]:
       if(str(v)==str(entry.varname)):
           return entry
  return None

class initAst(object):
    def __init__(self,stmts):
        self.stmts = stmts 

    def __repr__(self):
        init_str = "init:\n"
        for stmt in self.stmts:
            init_str += str(stmt)+"\n"
        return init_str
    
    def get_type(self):
        return inittype

class stmtAst(list):
    def __init__(self,stype):
        self.stype = stype 
    
    def get_type(self):
        return self.stype 
    def __repr__(self):
        return "lol"
class atomicAst(stmtAst):
    def __init__(self,wnum,stmts):
        self.stype = atomictype
        self.wnum = wnum
        self.stmts = stmts

    def __repr__(self):
        s = "atomic:"
        for stmt in self.stmts:
           s+= str(stmt)
        return s
    def get_type(self):
	return atomictype
class stageAsgnAst(stmtAst):
    def __init__(self,newst):
        self.stype = stctype 
        self.newst = newst

    def __repr__(self):
	return ("stage")
class asgnAst(stmtAst):
    def __init__(self,lvar,rexp):
        self.stype = asgntype 
        self.lvar = lvar
        self.rexp = rexp

    def __repr__(self):
        return str(self.lvar)+" = "+str(self.rexp)

class iteAst(stmtAst): 
    def __init__(self,cond,t,e):
        self.stype = 'ite'
        self.cond  = cond
        self.t = t
        self.e = e 

    def __repr__(self):
        s = "if "+str(self.cond)+"\n"
        for stmt in self.t:
          s+= str(stmt)
        if self.e is not None:
          s += "else"
          for stmt in self.e:
            s+= str(stmt)
	return s
class exprAst(list):
    def __init__(self,etype,lexp,rexp= None,op= None):
        self.etype = etype
        self.lexp = lexp 
        self.rexp = rexp
        self.op = op
   
    def __repr__(self):
        if self.op is None:
	   return str(self.lexp)
        if self.rexp is None:
	    return str(self.lexp)+ str(self.op)
        if self.lexp is None:
	    return str(self.op) + str(self.rexp)
	else:
            return "( "+str(self.lexp) +" "+ str(self.op) +" "+str(self.rexp)+" )"
    
    def get_type(self):
        return self.etype

class rvAst(list):
    def __init__(self,varname,access):
        self.varname = varname
        self.access  = access

    def __repr__(self):
        return str(self.varname)+"["+str(self.access)+"]"
    
    def get_type(self):
        return rvtype

class mfast(list):
    def __init__(self,modfunc,args,isstmt = False):
       self.modfunc = modfunc
       self.args = args
       self.isstmt = isstmt
    def __repr__(self):
        #modname = "s"#str(self.modfunc)[(str(self.modfunc)).index('.'):-1]
        m = str(self.modfunc)+"("
        if len(self.args) == 0 :
           m+= ")"
        else: 
          for i in range(len(self.args)-1) :
            m+= str(self.args[i])+", "
          m+= str(self.args[-1]) +")"
        return(m) 

    def get_type(self):
       return mfasttype 

class funcAst(list):
    def __init__(self,name,args,isstmt = False):
       self.name = name
       self.args = args
       self.isstmt = isstmt
    def __repr__(self):
        m = str(self.name)+"("
        if len(self.args) == 0 :
           m+= ")"
        else: 
          for i in range(len(self.args)-1) :
            m+= str(self.args[i])+", "
          m+= str(self.args[-1]) +")"
        return(m) 

    def get_type(self):
       return functype 

class eventAst(list):
    def __init__(self,name,pre,eff):
        self.name = name
        self.pre = pre
        self.eff = eff

    def __repr__(self):
        pre_str = "pre:\n"+str(self.pre)+"\n"
        eff_str = "eff:\n"
        for stmt in self.eff:
            eff_str += str(stmt)+"\n"
        return (self.name+":\n"+pre_str+eff_str)

    def get_type(self):
      return evnttype

class exitAst(list):
      def __init__(self):
          pass
      def get_type(self):
          return exittype
      def __repr__(self):
          return "return null;"
class condAst(list):
    def __init__(self,lexp,rexp=None,op=None):
        self.lexp = lexp
        self.rexp = rexp
        self.op = op
    
    def __repr__(self):
        if self.rexp is not None:
            return "( "+str(self.lexp) +" "+ str(self.op) +" "+str(self.rexp)+" )"
	if self.op is not None: 
	    return "(" + str(self.op)+"( " +str(self.lexp)+" )"+")"
	else: 
	    return ""+str(self.lexp)+""

    def get_type(self):
        return condtype
