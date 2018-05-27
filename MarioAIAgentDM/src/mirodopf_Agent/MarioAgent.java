package mirodopf_Agent;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.idsia.mario.engine.LevelScene;
import ch.idsia.mario.engine.sprites.Mario.STATUS;
import ch.idsia.mario.environments.Environment;
import de.novatec.mario.engine.generalization.Coordinates;
import de.novatec.mario.engine.generalization.Tile;
import de.novatec.mario.engine.generalization.Tiles.TileType;
import de.novatec.marioai.tools.LevelConfig;
import de.novatec.marioai.tools.MarioAiRunner;
import de.novatec.marioai.tools.MarioInput;
import de.novatec.marioai.tools.MarioKey;
import de.novatec.marioai.tools.MarioNtAgent;

public class MarioAgent extends MarioNtAgent {
	private static final int TARGET_OFFSET=Environment.HalfObsWidth*10;

	@Override
	public String getName() {
	
		return "GO GO Mario !";
	}

	@Override
	public MarioInput doAiLogic() {
		return getPath(getAStarCopyOfLevelScene());
	}
	
	private MarioInput getPath(LevelScene scene) {
		Node start=new Node(scene,null);
		List<Node> openSet;
		List<Node> closedSet;
		Map<Node,Double> gMap;
		Map<Node,Double> fMap;
		
		List<Tile> interactiveBlocks;
		
			openSet=new ArrayList<>();
			closedSet=new ArrayList<>();
			gMap=new HashMap<>();
			fMap=new HashMap<>();
			interactiveBlocks = getInteractiveBlocksOnScreen();
		
		openSet.add(start);
		gMap.put(start, 0.0); //cost for start is 0
		fMap.put(start, getDistanceFromTo(start.getX(), start.getY(), (float)start.getLevelScene().getLevelXExit()*16, 0)); //heuristic estimate
		
		Node actual=null;
		List<Node> nextNodes=new ArrayList<>();

		while(!openSet.isEmpty()) { //while open set is not empty or planing target reached
			
			//pick best node from open list with lowest cost
			actual=openSet.get(0);
			for(Node next: openSet)	if(fMap.get(next)  + getHeuristic(actual) <fMap.get(actual)) {
				actual=next;
			}
	
			if((actual.getX()>=scene.getMarioX()+TARGET_OFFSET)||actual.getLevelScene().getMarioStatus()==STATUS.WIN) {
				//setColor(Color.BLUE);
				break; //reached right field of view or win
			}
				
			nextNodes.clear();
			nextNodes=getAllNodes(actual, getAllInputs()); //generate next node list;
			
			openSet.remove(actual);
			closedSet.add(actual);

			//for each neighbour
				// if not in closed list ->continue
				// if not in open list ->add to open list
			for(Node neighbor: nextNodes) { 
				if(closedSet.contains(neighbor)) continue;
				
				if(!openSet.contains(neighbor)) openSet.add(neighbor);
				
				//get costs till now
				double tmpGScore=gMap.get(actual)+actual.getDistanceTo(neighbor);
				//check scores -> if path from actual to neighbor better than old way replace it
				if(gMap.get(neighbor)!=null&&tmpGScore>gMap.get(neighbor)) continue; //check if old path is better
				
				//else set this way
				neighbor.setParent(actual);
				gMap.put(neighbor, tmpGScore);
				fMap.put(neighbor,tmpGScore+getDistanceFromTo(neighbor.getX(), neighbor.getY(), (float)neighbor.getLevelScene().getLevelXExit()*16, 0));
			} //for each nextNodes
		} //while
		List<Node> tmp=reconstructPath(actual);
		if(tmp.size()>=2)return tmp.get(tmp.size()-2).getMarioInput();
		return getMarioInput();
	}
	
	protected List<Node> reconstructPath(Node last){
		List<Node> path=new ArrayList<>();
		Node parent=last;
		
		while(parent!=null) {
			path.add(parent);
			addCoordToDraw(new Coordinates(parent.getX(),parent.getY()));
			parent=parent.getParent();
		}
		return path;
	}
	
	//Exercise 4
	private List<MarioInput> getAllInputs(){
		List<MarioInput> inputs=new ArrayList<>();
		
		MarioInput input;
		

		input=new MarioInput();
		input.press(MarioKey.LEFT);
		inputs.add(input);
		
		input=new MarioInput();
		input.press(MarioKey.RIGHT);
		input.press(MarioKey.SPEED);
		input.press(MarioKey.JUMP);
		inputs.add(input);
		
		input=new MarioInput();
		input.press(MarioKey.RIGHT);
		input.press(MarioKey.SPEED);
		inputs.add(input);
		
		input=new MarioInput();
		input.press(MarioKey.RIGHT);
		inputs.add(input);
		
		input=new MarioInput();
		input.press(MarioKey.JUMP);
		inputs.add(input);
		
		
	
		
		//create more (useful) inputs and add them to the list
		
		return inputs;
	}
	
	//Exercise 5 
	private LevelScene tick(LevelScene actual, MarioInput input) {
		LevelScene scene = actual.getAStarCopy();

		scene.setMarioInput(input);
		scene.tick();
		
		return scene;
	}
	
	//Exercise 6
	private List<Node> getAllNodes(Node parent,List<MarioInput> inputs){
		List<Node> nodes=new ArrayList<>();
		LevelScene actual=parent.getLevelScene();
		
		for(MarioInput next: inputs) {
			LevelScene clone=tick(actual,next);
			if(clone.getMarioStatus() == STATUS.LOSE || clone.getTimesMarioHurt() > 0) {
				System.out.println("Hurt");
				continue;
			}
			nodes.add(new Node(clone,next));
		}
	
		return nodes;
	}
	
	//Exercise 7
	protected static double getDistanceFromTo(float x1,float y1,float x2, float y2) { // VERY simple heuristic, will get you to the target
		return Math.sqrt(Math.pow(x2-x1, 2)+Math.pow(y2-y1, 2));
	}
	
	private double getHeuristic(Node node) {
		/*
		for (Tile t : getInteractiveBlocksOnScreen()) {
			if (t.getType() == TileType.COIN) {
				if (Math.abs(node.getX()-t.getCoords().getX())< 1 && Math.abs(node.getY()-t.getCoords().getY())< 1) {
					System.out.println("COIN!!!");
					return - 10000;
				}
			}
		}
		*/
		if (node.getLevelScene().getMarioStatus() == STATUS.LOSE) {
			return 100000;
		}
		if (node.getLevelScene().getTimesMarioHurt() > 0) {
			System.out.println("Hurt");
			return 100000;
		}
		return -1*node.getLevelScene().getScore();
	}
	
	public static void main (String [] args) {
		MarioAiRunner.run(new MarioAgent(), LevelConfig.GOOD_LUCK, 24, 4, true, true, true);
	}
}