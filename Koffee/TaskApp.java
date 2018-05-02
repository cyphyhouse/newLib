package task;


import edu.illinois.mitra.cyphyhousemotion.MotionParameters;
import edu.illinois.mitra.cyphyhousemotion.RRTNode;
import edu.illinois.mitra.cyphyhousemotion.MotionParameters.COLAVOID_MODE_TYPE;
import edu.illinois.mitra.cyphyhouseobjects.ItemPosition;
import edu.illinois.mitra.cyphyhouseobjects.ObstacleList;
import edu.illinois.mitra.cyphyhouseobjects.PositionList;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.file.*;
import java.util.stream.Stream;

import edu.illinois.mitra.cyphyhousegvh.GlobalVarHolder;
import edu.illinois.mitra.cyphyhouseinterfaces.LogicThread;

public class TaskApp extends LogicThread  {

   private static final String TAG = "TaskApp";
   private int numBots;
   private int pid;
   
   
   public Task(GlobalVarHolder gvh)  {
   
      super(gvh);
      String intValue = name.replaceAll("[^0-9]", "");
      pid = Integer.parseInt(intValue);
      numBots = gvh.id.getParticipants().size();
      MotionParameters.Builder settings = new MotionParameters.Builder();
      settings.COLAVOID_MODE(COLAVOID_MODE_TYPE.USE_COLAVOID);
      MotionParameters param = settings.build();
      gvh.plat.moat.setParameters(param);
      
   }
   @Override
   public List<Object> callStarL()  {
   
      while(true)  {
      
         
      }
      
   }
   
   
}
