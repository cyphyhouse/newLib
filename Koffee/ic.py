#-*- coding: utf-8 -*-
"""
Not all generated Java files need all possible packages that can be used by a
physical agent running a Koord program. We use the *ic.py* file to store the
dictionary (*IC, or import-code*) we refer to in order to only import the minimal
set for compiling a generated java file.

The IC dictionary
-----------------
+--------------------+-------------------------------------------+
|requirements        | imports                                   |
+====================+===========================================+
|java defaults       | net,nio,util,lang,                        |
|                    | nio.file,util.stream.Stream               |
+--------------------+-------------------------------------------+
|project defaults    |gvh.GlobalVarHolder,interfaces.LogicThread |
+--------------------+-------------------------------------------+
|shared variable     |functions.DSMMultipleAttr,interfaces.DSM,  |
|                    |interfaces.MutualExclusion,                |
|                    |functions.GroupSetMutex                    |
+--------------------+-------------------------------------------+
|motion              |motion.MotionParameters,motion.RRTNode,    |
|                    |motion.MotionParameters.COLAVOID_MODE_TYPE,|
|                    |objects.ItemPosition,objects.ObstacleList, |
|                    |objects.PositionList                       |
+--------------------+-------------------------------------------+

The cyphyhouse module imports actually should be imported with the project prefix.
For instance, the generated java statement for importing gvh will be
*"import edu.illinois.mitra.cyphyhouse.gvh.GlobalVarHolder;"*

.. module:: imports
   :synopsis: Adding required imports for the generated Java code
.. moduleauthor:: Ritwika Ghosh <rghosh9@illinois.edu>


"""
from generic import mk_stmt

#IMPORTANT: UPDATE DICTIONARY AND DOCUMENTATION TOGETHER. creating import dictionary
IC = {}
#Prefix dictionary
PC = {}

#append required default java import lines here.
JAVEDEF = []
JAVEDEF.append("java.net.*")
JAVEDEF.append("java.io.*")
JAVEDEF.append("java.util.*")
JAVEDEF.append("java.lang.*")
JAVEDEF.append("java.nio.file.*")
JAVEDEF.append("java.util.stream.Stream")

#assign default java import dictionary entry
IC['javadef'] = JAVEDEF
PC['javadef'] = ""

#append required default project import lines here.
DEFAULT = []
DEFAULT.append("gvh.GlobalVarHolder")
DEFAULT.append("interfaces.LogicThread")

#assign default project import dictionary entry.
IC['default'] = DEFAULT
PC['default'] = "edu.illinois.mitra.cyphyhouse"

#append required shared variable imports here.
SHARED = []
SHARED.append("functions.DSMMultipleAttr")
SHARED.append("interfaces.DSM")
SHARED.append("interfaces.MutualExclusion")
SHARED.append("functions.GroupSetMutex")

#assign shared variable import dictionary entry for both allwrite and allread.
IC['allwrite'] = SHARED
IC['allread'] = SHARED
PC['allread'] = "edu.illinois.mitra.cyphyhouse"
PC['allwrite'] = "edu.illinois.mitra.cyphyhouse"


#append motion module imports here.
MOTION = []
MOTION.append("motion.MotionParameters")
MOTION.append("motion.RRTNode")
MOTION.append("motion.MotionParameters.COLAVOID_MODE_TYPE")
MOTION.append("objects.ItemPosition")
MOTION.append("objects.ObstacleList")
MOTION.append("objects.PositionList")

#assign motion module imports here.
IC['Motion'] = MOTION
PC['Motion'] = "edu.illinois.mitra.cyphyhouse"


def gen_ic(flag):
    """
    This is a function for generating the java import statements corresponding to a flag.

    Args:
        flag (str) : the flag to be passed to include the import statements.
    Returns:
        str : the list of import statements.

    """
    import_list = IC[flag]
    import_statement = ""
    for item in import_list:
        import_statement += mk_stmt("import "+PC[flag]+item)
    return import_statement
