#-*- coding: utf-8 -*-
"""
We include the mandatory declarations for the generated java programs in the file *mdecls.py*.

- Required :
    1. private int numBots : Number of robots in the system.
    2. private int pid : The pid of the robot running this copy of the program.
    3. private static final String TAG appname : logging tag.
       TODO: document the need for this better
- Generated :
    1. private DSM dsm : if shared variables are being used.
    2. private MutualExclusion mutex_i : for every mutual exclusion block required.
"""
import generic

def mandatory_decls(input_ast, wnum):
    '''
    function for returning the mandatory declarations as a string
    '''
    return_string = ""
    inputast_name = input_ast.get_name()
    tag_string = "private static final String TAG = " + '"' + inputast_name + 'App"'
    return_string += generic.mk_stmt(tag_string)
    for i in range(0, wnum):
        return_string += generic.mk_stmt("private MutualExclusion mutex" + str(i))
    if input_ast.has_flag('allwrite') or input_ast.has_flag('allread'):
        return_string += generic.mk_stmt('private DSM dsm')
    return_string += generic.mk_stmt('private int numBots')
    return_string += generic.mk_stmt('private int pid')
    return return_string

def mandatory_inits(input_ast, wnum):
    '''
    function for generating mandatory initializations.
    '''
    return_string = generic.mk_stmt("super(gvh)")
    return_string += generic.mk_stmt('String intValue = name.replaceAll("[^0-9]", "")')
    return_string += generic.mk_stmt("pid = Integer.parseInt(intValue)")
    return_string += generic.mk_stmt("numBots = gvh.id.getParticipants().size()")
    if input_ast.has_flag('allwrite') or input_ast.has_flag('allread'):
        return_string += generic.mk_stmt('dsm = new DSMMultipleAttr(gvh)')
    for i in range(0, wnum):
        mutex_str = ("mutex" + str(i) + " = new GroupSetMutex(gvh," + str(i) + ")")
        return_string += generic.mk_stmt(mutex_str)

    if input_ast.has_flag('Motion'):
        m_str = "MotionParameters.Builder settings = new MotionParameters.Builder()"
        return_string += generic.mk_stmt(m_str)
        m_str = "settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID)"
        return_string += generic.mk_stmt(m_str)
        return_string += generic.mk_stmt("MotionParameters param = settings.build()")
        return_string += generic.mk_stmt("gvh.plat.moat.setParameters(param)")
    return return_string


