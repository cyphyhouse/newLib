package testSim.follow;

import edu.illinois.mitra.cyphyhouse.objects.ItemPosition;

public class Task {
    public ItemPosition location;
    public int assigned;
    public boolean completed;
    public int id;

    public Task(ItemPosition location,int id){
        this.location = location;
        this.id = id;
        this.assigned = -1;
        this.completed = false;
    }

    public void assign(int i){
        this.assigned = i;
    }

    public boolean isAssigned() {
        if (this.assigned != -1){
            return false;
        }
        return true;
    }

    public void complete(){
        this.completed = true;
    }

    public String toString() {
        String s = "";
        s += "task" + this.id + " at " + this.location.toString();
        if (this.assigned == -1) {
            s += " is unassigned";
        }
        else {
            s += " is assigned to robot "+ this.assigned;
        }
        return s;
    }
}
