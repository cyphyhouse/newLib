


from Tkinter import Tk, Label, Button, Canvas, Radiobutton, Toplevel, OptionMenu, StringVar, Message, Entry, IntVar, Checkbutton, PhotoImage
from time import sleep
import numpy as np
import socket
import sys
from threading import Thread


class MapUI:
    
    def __init__(self, master):

        def center(win):
            win.update_idletasks()
            width = win.winfo_width()
            height = win.winfo_height()
            x = (win.winfo_screenwidth() // 4) - (width // 2) + 40
            y = (win.winfo_screenheight() // 4) - (height // 2) + 40
            win.geometry('{}x{}+{}+{}'.format(width, height, x, y))

        def callback(event):
            event.widget.focus_set()
            print "clicked at", event.x, event.y

            if self.add_tasks_flg.get() == 1:
                self.enter_task(event)


        master.title("Map Interface")
        master.minsize(width=1000, height=750)
        master.maxsize(width=1000, height=750)
        master.config(bg=BKG_COLOUR)
        self.master = master

        # Canvas for overlaying map
        global MAP_CANVAS
        MAP_CANVAS = Canvas(master, width=CANVAS_W, height=CANVAS_H, bg='gray85', highlightthickness=0)
        MAP_CANVAS.pack(side='right',padx=50)
        MAP_CANVAS.bind("<Button-1>", callback)
        global CANVAS_PTR
        CANVAS_PTR = MAP_CANVAS
        self.master.update()
        w = MAP_CANVAS.winfo_width()
        h = MAP_CANVAS.winfo_height()
        # Overlay a grid
        for i in range(0,w,SQ_SIZE):
            if i != 0:
                MAP_CANVAS.create_line(i,0,i,h,dash=1)
        for i in range(0,h,SQ_SIZE):
            if i != 0:
                MAP_CANVAS.create_line(0,i,w,i,dash=1)


        # Load in flame icon from flame.gif
        self.flame_icon = PhotoImage(file="flame.gif")
        # Load in the drone icon from drone.gif
        global DRONE_ICON
        DRONE_ICON = PhotoImage(file="drone.gif")

        buttons_frame = Canvas(master, width=163, height=140, bg=BUTTONS_BKG_COLOUR, highlightthickness=1, highlightbackground='dim grey')
        buttons_frame.place(x=40,y=230)

        # Define UI buttons
        self.add_tasks_flg = IntVar()
        self.add_tasks_b = Checkbutton(master, text="Add Tasks", variable=self.add_tasks_flg, highlightbackground=BUTTONS_BKG_COLOUR, background=BUTTONS_BKG_COLOUR)
        self.add_tasks_b.place(x=77,y=270)

        self.clear_wp_b = Button(master, text='Clear Tasks', command=self.clear_wp, highlightbackground=BUTTONS_BKG_COLOUR)
        self.clear_wp_b.config(width=10)
        self.clear_wp_b.place(x=65, y=300)
        
        '''
        self.gen_wp_file_b = Button(master, text='Generate Waypoints File', command=self.gen_wp_file, highlightbackground=BKG_COLOUR)
        self.gen_wp_file_b.config(width=20)
        self.gen_wp_file_b.place(x=20, y=250)
        '''

        '''
        self.land_b = Button(master, text='Land', command=self.land, highlightbackground=BUTTONS_BKG_COLOUR)
        self.land_b.config(width=10)
        self.land_b.place(x=65, y=350)
        '''


        # Set up coordinate system conversion and display corners of room:
        file_obj  = open('antenna_locations.txt', 'r')
        anchors = []
        for line in file_obj:
            cur_anchors = map(float, line.split())
            anchors.append(cur_anchors)
        file_obj.close()
        anchors = (np.array(anchors)).T

        # Find largest (abs) x and y values to use a reference for conversion ratio
        x_vals = anchors[0]
        largest_x_val = x_vals[np.argmax(abs(x_vals))]
        y_vals = anchors[1]
        largest_y_val = y_vals[np.argmax(abs(y_vals))]

        if largest_x_val > largest_y_val:
            largest_y_val = largest_x_val
        else:
            largest_x_val = largest_y_val

        global m_per_pixel_x
        m_per_pixel_x = float(largest_x_val/(CANVAS_W/2))
        global m_per_pixel_y
        m_per_pixel_y = float(largest_y_val/(CANVAS_H/2))

        # Place antenna (anchors) on UI
        anchors = anchors.T
        for cur_anchor in anchors:
            x_pixel_loc = cur_anchor[0] / m_per_pixel_x + CANVAS_W/2
            y_pixel_loc = -1*(cur_anchor[1] / m_per_pixel_y) + CANVAS_H/2

            # Draw antenna @ location
            global ANTENNA_LIST
            antenna_id = MAP_CANVAS.create_oval(x_pixel_loc-15,y_pixel_loc-15,x_pixel_loc+15,y_pixel_loc+15,fill='red')
       

        self.master.update()

    global SQ_SIZE 
    SQ_SIZE = 20
    global BKG_COLOUR
    BKG_COLOUR = 'gray95'
    global BUTTONS_BKG_COLOUR
    BUTTONS_BKG_COLOUR = 'grey66'
    global CANVAS_W
    CANVAS_W = 700
    global CANVAS_H
    CANVAS_H = 700
    global TASK_LIST
    TASK_LIST = None
    global m_per_pixel_x
    m_per_pixel_x = None
    global m_per_pixel_y
    m_per_pixel_y = None
    global NEW_TASK_FLAG
    NEW_TASK_FLAG = False
    global ANTENNA_LIST
    ANTENNA_LIST = None
    global DRONE_ICON 
    DRONE_ICON = None
    global UI_WP_LIST
    UI_WP_LIST = None
    global MAP_CANVAS
    MAP_CANVAS = None



    flame_icon = None
    ui_wp_list = None
    add_wp_flag = False
    task_id = 0
    add_tasks_flg = None
    

    def add_tasks(self):
        print "adding tasks"
        # function imp here
        self.add_wp_flag = True
        MAP_CANVAS.config(cursor='pencil')


    def clear_wp(self):
        print "clear tasks"
        global TASK_LIST
        TASK_LIST = None
        for element_id in UI_WP_LIST:
            MAP_CANVAS.delete(element_id[0])
        global UI_WP_LIST
        UI_WP_LIST = None

    '''
    def gen_wp_file(self):
        print "generate wp file"
        # function imp here
    '''

    def land(self):
        # Send a new task with position (0,0,0) z=0 tells drone to land
        print("land")



    def enter_task(self, event):
        # Determine square (top left corner coords):
        w_start = event.x - event.x%SQ_SIZE
        h_start = event.y - event.y%SQ_SIZE

        #Translate pixel location to physical location
        x_pixel = event.x
        y_pixel = event.y
        # Find out how many pixels from center:
        x_pixel = x_pixel - CANVAS_W/2
        x_physical = x_pixel*m_per_pixel_x

        #vertical case, note this is flipped
        y_pixel = y_pixel - CANVAS_W/2
        y_pixel = -1*y_pixel
        y_physical = y_pixel*m_per_pixel_y

        try:
            # Add to task list
            global TASK_LIST
            if TASK_LIST == None:
                TASK_LIST = [[x_physical, y_physical, 1.5, self.task_id]]
                TASK_LIST = [[x_physical, y_physical, 1.5, self.task_id]]
                global NEW_TASK_FLAG
                NEW_TASK_FLAG = True
            else:
                TASK_LIST.append([x_physical, y_physical, 1.5, self.task_id])
                global NEW_TASK_FLAG
                NEW_TASK_FLAG = True

            # Indicate task in UI
            element_id = MAP_CANVAS.create_image(event.x, event.y, image=self.flame_icon)

            if UI_WP_LIST == None:
                global UI_WP_LIST
                UI_WP_LIST = [[element_id,self.task_id]]
            else:
                UI_WP_LIST.append([element_id,self.task_id])
        except:
            print("Invalid Task Entry")



        MAP_CANVAS.config(cursor='arrow')
        self.add_wp_flag = False

        print(TASK_LIST)
        
        self.task_id = self.task_id + 1
        

    def cancel_task(self):
        self.top.destroy()



global PREV_ROBOT_LOCATION
PREV_ROBOT_LOCATION = None

def socket_loop():


    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    ##s = socket.socket()        
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) 

    # Define the port on which you want to connect
    port = 12345

    '''
    ### Commented out block to recieve Vicon data for now so UI doesn't ####
    ### stall when not connected to the Vicon                           ####
    s.connect(('192.168.1.15', port))

    # receive data from the Vicon
    position = s.recv(1024)
    position = map(float, position.split())
    position[0] = (position[0]/m_per_pixel_x)+CANVAS_W/2
    position[1] = -1*((position[1]/m_per_pixel_y))+CANVAS_H/2
    
    if PREV_ROBOT_LOCATION != None:
        CANVAS_PTR.delete(PREV_ROBOT_LOCATION)

    global PREV_ROBOT_LOCATION
    PREV_ROBOT_LOCATION = CANVAS_PTR.create_image(position[0], position[1], image=DRONE_ICON)
    
    s.close()
    '''
    
    
    #Send new task to reciver on drone, the reciever will write to tasks.txt
    if NEW_TASK_FLAG == True:
       
        with open("tasks.txt",'a') as file_obj:
            new_task = TASK_LIST[len(TASK_LIST)-1]
            task_string = ['{:.2f}'.format(x) for x in new_task]
            message = " ".join(task_string)
            message = message + "\n"
            file_obj.write(message.encode())
            global NEW_TASK_FLAG
            NEW_TASK_FLAG = False
       
    '''
    #Check if any tasks have been done, if so, remove flame icon from screen
    with open("tasks.txt",'r') as file_obj:
        for line in file_obj:
            cur_line = line.split()
            done_flag = float(cur_line[1])
            if done_flag == 1.0:
                task_id = float(cur_line[0])
                for element_id in UI_WP_LIST:
                    if element_id[1] == task_id:
                        MAP_CANVAS.delete(element_id[0])
                        UI_WP_LIST.remove(element_id)
                        # ADD SOME OTHER STUFF HERE TO REMOVE FROM TASK LIST
    '''

    '''
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)       
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1) 
        s.connect(('', 12345))
        new_task = TASK_LIST[len(TASK_LIST)-1]
        task_string = ['{:.2f}'.format(x) for x in new_task]
        message = " ".join(task_string)
        s.send(message.encode())
        global NEW_TASK_FLAG
        NEW_TASK_FLAG = False
        s.close()
    '''
    
    root.after(150, socket_loop)


root = Tk()
my_gui = MapUI(root)
socket_loop()
root.mainloop()


















