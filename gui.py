


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
                # Select number of robots
                # Define elements of pop up window
                self.top = Toplevel()

                self.num_robots = StringVar(self.top)
                self.num_robots.set("1") # default value
                w = OptionMenu(self.top, self.num_robots, '1','2','3','4').grid(row=0,column=1)
                text1 = Message(self.top, text="Number of robots:", width = 150).grid(row=0,column=0)

                self.e = Entry(self.top, width=10)
                self.e.grid(row=1,column=1)
                text2 = Message(self.top, text="Task duration:", width = 150).grid(row=1,column=0)
                text3 = Message(self.top, text="(s)", width=60).grid(row=1,column=2)

                newline = Message(self.top, text=" ").grid(row=2)

                button = Button(self.top, text='Enter', command=lambda: self.enter_task(event)).grid(row=3,column=1)
                button_cancel = Button(self.top, text='Cancel', command=self.cancel_task).grid(row=3,column=2)

                center(self.top)


        master.title("Map Interface")
        master.minsize(width=1000, height=750)
        master.maxsize(width=1000, height=750)
        master.config(bg=BKG_COLOUR)
        self.master = master

        # Canvas for overlaying map
        self.map_canvas = Canvas(master, width=CANVAS_W, height=CANVAS_H, bg='gray85', highlightthickness=0)
        self.map_canvas.pack(side='right',padx=50)
        self.map_canvas.bind("<Button-1>", callback)
        global CANVAS_PTR
        CANVAS_PTR = self.map_canvas
        self.master.update()
        w = self.map_canvas.winfo_width()
        h = self.map_canvas.winfo_height()
        # Overlay a grid
        for i in range(0,w,SQ_SIZE):
            if i != 0:
                self.map_canvas.create_line(i,0,i,h,dash=1)
        for i in range(0,h,SQ_SIZE):
            if i != 0:
                self.map_canvas.create_line(0,i,w,i,dash=1)


        # Load in flame icon from flame.gif
        self.flame_icon = PhotoImage(file="flame.gif")
        # Load in the drone icon from drone.gif
        global DRONE_ICON
        DRONE_ICON = PhotoImage(file="drone.gif")

        buttons_frame = Canvas(master, width=163, height=230, bg=BUTTONS_BKG_COLOUR, highlightthickness=1, highlightbackground='dim grey')
        buttons_frame.place(x=40,y=200)

        # Define UI buttons
        self.add_tasks_flg = IntVar()
        self.add_tasks_b = Checkbutton(master, text="Add Tasks", variable=self.add_tasks_flg, highlightbackground=BUTTONS_BKG_COLOUR, background=BUTTONS_BKG_COLOUR)
        self.add_tasks_b.place(x=77,y=240)

        self.clear_wp_b = Button(master, text='Clear Tasks', command=self.clear_wp, highlightbackground=BUTTONS_BKG_COLOUR)
        self.clear_wp_b.config(width=10)
        self.clear_wp_b.place(x=65, y=270)
        
        '''
        self.gen_wp_file_b = Button(master, text='Generate Waypoints File', command=self.gen_wp_file, highlightbackground=BKG_COLOUR)
        self.gen_wp_file_b.config(width=20)
        self.gen_wp_file_b.place(x=20, y=250)
        '''

        self.land_b = Button(master, text='Land', command=self.land, highlightbackground=BUTTONS_BKG_COLOUR)
        self.land_b.config(width=10)
        self.land_b.place(x=65, y=350)


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
            antenna_id = self.map_canvas.create_oval(x_pixel_loc-15,y_pixel_loc-15,x_pixel_loc+15,y_pixel_loc+15,fill='red')
       

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


    flame_icon = None
    ui_wp_list = None
    #task_list = None
    add_wp_flag = False
    task_id = 0
    add_tasks_flg = None
    

    def add_tasks(self):
        print "adding tasks"
        # function imp here
        self.add_wp_flag = True
        self.map_canvas.config(cursor='pencil')


    def clear_wp(self):
        print "clear tasks"
        global TASK_LIST
        TASK_LIST = None
        for element_id in self.ui_wp_list:
            self.map_canvas.delete(element_id[0])
        self.ui_wp_list = None

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
	            TASK_LIST = [[self.task_id, int(self.num_robots.get()), float(self.e.get()), x_physical, y_physical]]
	            global NEW_TASK_FLAG
	            NEW_TASK_FLAG = True
	        else:
	            TASK_LIST.append([self.task_id, int(self.num_robots.get()), float(self.e.get()), x_physical, y_physical])
	            global NEW_TASK_FLAG
	            NEW_TASK_FLAG = True

	        # Indicate task in UI
	        element_id = self.map_canvas.create_image(event.x, event.y, image=self.flame_icon)
	        if self.ui_wp_list == None:
	            self.ui_wp_list = [[element_id]]
	        else:
	            self.ui_wp_list.append([element_id])
       	except:
       		print("Invalid Task Entry")



        self.map_canvas.config(cursor='arrow')
        self.add_wp_flag = False

        print(TASK_LIST)
        
        self.task_id = self.task_id + 1
        self.top.destroy()

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

    
    ### Commented out block to recieve Vicon data for now so UI doesn't ####
    ### stall when not connected to the Vicon 							####
    s.connect(('', port))

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
    
    
    #Send update message with tasks
    if NEW_TASK_FLAG == True:
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
	
    root.after(150, socket_loop)


root = Tk()
my_gui = MapUI(root)
socket_loop()
root.mainloop()


















