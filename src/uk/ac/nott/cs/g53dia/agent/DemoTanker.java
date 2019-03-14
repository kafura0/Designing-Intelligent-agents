package uk.ac.nott.cs.g53dia.agent;

import uk.ac.nott.cs.g53dia.library.*;
import java.util.*;
import java.util.Arrays;

public class DemoTanker extends Tanker {
	//initialize tank position x n y
	private int curCoorX = 0;
	private int curCoorY = 0;

	//get all initial points of objects..search action and view
	private ArrayList<int[]> initialPoints = new ArrayList<>();
	private boolean isSeekingTask = true;
	private int walkView = 0;

	//store positions of objects
	private ArrayList<int[]> allFuelPumps = new ArrayList<>();
	private ArrayList<int[]> allStations = new ArrayList<>();
	private ArrayList<int[]> tasksList = new ArrayList<>();
	private ArrayList<int[]> allWells = new ArrayList<>();

	private boolean isToTask = true;


	public DemoTanker() {
		//constructor- set defaults
		// Manually set four points which are followed by agent in two cases:
		//  1. At the start of agent.to detect and set limit range of env around the root
		//  2. When there's no detected task by tanker,follow this path
		initialPoints.add(new int[]{0,0});
		initialPoints.add(new int[]{20,20});
		initialPoints.add(new int[]{20,-20});
		initialPoints.add(new int[]{0,0});

		// Add FuelPump at the center of the env
		allFuelPumps.add(new int[]{0,0});
	}
	//causes actions
	public Action senseAndAct(Cell[][] view,  boolean actionFailed,long timestep) {
		if(timestep == 10000){
			System.out.println("Timestep: " + timestep + "; Final Score: " + getScore() + ";");
		}
		//get initial position of all objects
		initialState(view);

		// agent is detecting env else doing an action
		if(isSeekingTask){
			//deliberation
			return seekingTask(view, timestep);
		}else{
			//reacting
			return prioritizeActions(view, timestep);
		}
	}
	//Detect all stations, wells, fuel pumps and tasks
	//get information
	private void initialState(Cell[][] view){
		for(int i = 0 ; i < view.length ; i++){
			for(int j = 0; j < view[i].length ; j++){
				//tanker can view 20 cells within its vicinity
				int[] curView = new int[]{i + curCoorX-20, -j + curCoorY+20};

				// Store all detected stations, wells, fuel pumps and tasks
				if (Math.max(Math.abs(curView[0]), Math.abs(curView[1])) <= 99){
					if (view[i][j] instanceof Station) {
						if (memoryList(allStations, curView) == -1){
							allStations.add(curView);
						}
						Station curStation = (Station) view[i][j];
						if (curStation.getTask() != null){
							int[] taskDetails = new int[]{curView[0], curView[1], curStation.getTask().getWasteAmount()};
							if (memoryList(tasksList, taskDetails) == -1){
								tasksList.add(taskDetails);
							}
						}
					} else if (view[i][j] instanceof Well && memoryList(allWells, curView) == -1) {
						allWells.add(curView);
					} else if (view[i][j] instanceof FuelPump && memoryList(allFuelPumps, curView) == -1) {
						allFuelPumps.add(curView);
					} else {
						continue;
					}
				}
			}
		}
	}
	//keeps memory of task, points travelled
	private int memoryList(ArrayList<int[]> seenList, int[] currentPoint){
		for (int i = 0; i < seenList.size(); i++){
			if (Arrays.equals(seenList.get(i), currentPoint)){
				return i;
			}
		}
		return -1;
	}
	//initialize detect environment around
	private Action seekingTask(Cell[][] view, long timestep){
		if(walkView >= initialPoints.size()){
			isSeekingTask = false;
			return senseAndAct(view, false,timestep);
		}
		int[] targetPos = initialPoints.get(walkView);
		if(targetPos[0] == curCoorX && targetPos[1] == curCoorY){
			walkView++;
			if(getCurrentCell(view) instanceof FuelPump){
				if(getFuelLevel() == 100) {
					return senseAndAct(view, false,timestep);
				} else {
					return new RefuelAction();
				}
			} else {
				return senseAndAct(view, false,timestep);
			}
		} else {
			return moveTowards(targetPos);
		}
	}
	private Action seekFor(Cell[][] view, long timestep){
		isSeekingTask = true;
		walkView = 0;
		return seekingTask(view, timestep);
	}

	private Action moveTowards(int[] targetPos){
		int horizontalDifference = targetPos[0] - curCoorX;
		int verticalDifference = targetPos[1] - curCoorY;
		int verticalMovement, horizontalMovement;

		if(horizontalDifference > 0){
			horizontalMovement = 1;
		} else if (horizontalDifference == 0){
			horizontalMovement = 0;
		} else {
			horizontalMovement = -1;
		}

		if(verticalDifference > 0){
			verticalMovement = 1;
		} else if (verticalDifference == 0){
			verticalMovement = 0;
		} else {
			verticalMovement = -1;
		}

		int directionToGo = 0;
		switch (horizontalMovement) {
			case 0:
				if(verticalMovement == 1){
					directionToGo = 0;
				} else {
					directionToGo = 1;
				}
				break;
			case 1:
				if(verticalMovement == 1){
					directionToGo = 4;
				} else if (verticalMovement == 0) {
					directionToGo = 2;
				} else {
					directionToGo = 6;
				}
				break;
			case -1:
				if(verticalMovement == 1){
					directionToGo = 5;
				} else if (verticalMovement == 0) {
					directionToGo = 3;
				} else {
					directionToGo = 7;
				}
				break;
		}

		moveDirection(directionToGo);
		//throws OutOfFuelException
		return new MoveAction(directionToGo);
	}
	private int shortestDistanceTo(ArrayList<int[]> indicesList, int[] point){
		int furthestGoableDistance = 100;
		int closestIndex = -1;
		for(int i = 0;i < indicesList.size();i++){
			int[] positionGoable = indicesList.get(i);
			int distanceBetween = Math.max(Math.abs(positionGoable[0] - point[0]),Math.abs(positionGoable[1] - point[1]));
			if(distanceBetween <= furthestGoableDistance){
				furthestGoableDistance = distanceBetween;
				closestIndex = i;
			}
		}
		return closestIndex;
	}

	private Action prioritizeActions(Cell[][] view, long timestep){

		// taskAmount >= 1 ie station has generated a task
		if(tasksList.size() >= 1){
			// Find nearest task going from current tank position
			int taskChosenIndex = shortestDistanceTo(tasksList, new int[]{curCoorX, curCoorY});

			// -1 means there is no task that can be reached
			// from current tanker position
			//ensure fuel does not run out
			if(taskChosenIndex == -1){
				int[] root = {0,0};
				// Find nearest fuelPump going from current tank position
				int[] nearestFuelpumpGoingFromCurrent = allFuelPumps.get(shortestDistanceTo(allFuelPumps, new int[]{curCoorX, curCoorY}));
				int distanceBetweenRootAndCurrent = Math.max(Math.abs(root[0] - curCoorX),Math.abs(root[1] - curCoorY));

				if(getFuelLevel() > distanceBetweenRootAndCurrent){
					return seekFor(view, timestep);
				} else {
					if(getCurrentCell(view) instanceof FuelPump){
						if(getFuelLevel() == 100) {
							return seekFor(view, timestep);
						} else {
							System.out.println("no task reachable ..refuel keep exploring ");
							return new RefuelAction();
						}
					} else {
						return moveTowards( nearestFuelpumpGoingFromCurrent);
					}
				}
			}

			int[] taskToDo = tasksList.get(taskChosenIndex);
			int taskToDoX = taskToDo[0];
			int taskToDoY = taskToDo[1];

			// Find nearest fuelPump going from taskToGo
			int[] nearestFPFromTask = allFuelPumps.get(shortestDistanceTo(allFuelPumps, taskToDo));

			// Find nearest fuelPump going from current tank position
			int[] nearestFPFromCurrent = allFuelPumps.get(shortestDistanceTo(allFuelPumps, new int[]{curCoorX, curCoorY}));

			// Find nearest well going from taskToGo
			int[] nearestWellFromTask = allWells.get(shortestDistanceTo(allWells, taskToDo));

			// Find nearest fuelPump going from well
			int[] nearestFPGoingFromWell = allFuelPumps.get(shortestDistanceTo(allFuelPumps, nearestWellFromTask));

			// Find nearest well going from current tank position
			int[] nearestWellFromCurrent = allWells.get(shortestDistanceTo(allWells, new int[]{curCoorX, curCoorY}));

			// Find nearest fuelPump going from well
			//int[] nearestFuelpumpGoingFromWell2 = allFuelPumps.get(shortestDistanceTo(allFuelPumps, nearestWellFromCurrent));

			int distanceBetweenTankAndTask = Math.max(Math.abs(taskToDo[0] - curCoorX),Math.abs(taskToDo[1] - curCoorY));
			int fuelLeftAfterTask = getFuelLevel() - distanceBetweenTankAndTask;
			int distanceBetweenTaskAndFuelPump = Math.max(Math.abs(taskToDo[0] - nearestFPFromTask[0]),Math.abs(taskToDo[1] - nearestFPFromTask[1]));

			//tank collects waste
			if(getWasteCapacity() > taskToDo[2]){
				// tank to task station
				//check fuel level to get to fp after task completion
				if(fuelLeftAfterTask > distanceBetweenTaskAndFuelPump){
					if(getCurrentCell(view) instanceof Station){
						System.out.println("1.TankAt:" + "(" + curCoorX + "," + curCoorY + ")");
						if (curCoorX != taskToDo[0] || curCoorY != taskToDo[1]) {
							return moveTowards( taskToDo);
						} else {
							System.out.println(" AT TASK STATION AND COLLECTING");
							Station currentCellStation = (Station) getCurrentCell(view);
							Task currentTask = currentCellStation.getTask();
							tasksList.remove(taskChosenIndex);
							return new LoadWasteAction(currentTask);
						}
					} else {
						System.out.println("1.TO TASK STATION:" + "(" + taskToDoX + "," + taskToDoY + ") TankAt:" + "(" + curCoorX + "," + curCoorY + ")");
						return moveTowards( taskToDo);
					}
				}
				// tank refuels while doing collection tasks
				// tank > station > fp
				else {
					if(getCurrentCell(view) instanceof FuelPump){
						System.out.println("2.FP:" + "(" + nearestFPFromCurrent[0] + "," + nearestFPFromCurrent[1] + ")");
						if(getFuelLevel() == 100) {
							return seekFor(view, timestep);
						} else {
							System.out.println(" TankAt FP:" + "(" + curCoorX + "," + curCoorY + ") REFUELLING");
							return new RefuelAction();
						}
					} else {
						System.out.println("2.TO FP:" + "(" + nearestFPFromCurrent[0] + "," + nearestFPFromCurrent[1] + ") TankAt:" + "(" + curCoorX + "," + curCoorY + ")");
						return moveTowards(nearestFPFromCurrent);
					}
				}
			}
			//-----------------------------------------------------------------------------
			// tankCapacity() == taskAmount
			//tank > task > well > fp
			else if(getWasteCapacity() == taskToDo[2]) {
				int distanceBetweenTaskAndWell = Math.max(Math.abs(taskToDo[0] - nearestWellFromTask[0]),Math.abs(taskToDo[1] - nearestWellFromTask[1]));
				int distanceBetweenWellAndFuelPump = Math.max(Math.abs(nearestFPGoingFromWell[0] - nearestWellFromTask[0]),Math.abs(nearestFPGoingFromWell[1] - nearestWellFromTask[1]));
				int fuelAfterTaskWell = getFuelLevel() - distanceBetweenTankAndTask - distanceBetweenTaskAndWell;

				// tank > task
				if(fuelAfterTaskWell > distanceBetweenWellAndFuelPump) {
					if (isToTask) {
						if (getCurrentCell(view) instanceof Station) {
							if (curCoorX != taskToDo[0] || curCoorY != taskToDo[1]) {
								return moveTowards(taskToDo);
							} else {
								Station currentCellStation = (Station) getCurrentCell(view);
								Task currentTask = currentCellStation.getTask();
								tasksList.remove(taskChosenIndex);

								isToTask = false;
								return new LoadWasteAction(currentTask);
							}
						} else {
							return moveTowards(taskToDo);
						}
					} else {
						// tank > task > well
						if (getCurrentCell(view) instanceof Well) {

							isToTask = true;
							return new DisposeWasteAction();
						} else {
							return moveTowards(nearestWellFromTask);
						}
					}
				}
				// tank > task > fp
				else if(fuelLeftAfterTask > distanceBetweenTaskAndFuelPump) {
					if(getCurrentCell(view) instanceof Station){
						if (curCoorX != taskToDo[0] || curCoorY != taskToDo[1]) {
							return moveTowards(taskToDo);
						} else {
							Station currentCellStation = (Station) getCurrentCell(view);
							Task currentTask = currentCellStation.getTask();
							tasksList.remove(taskChosenIndex);
							return new LoadWasteAction(currentTask);
						}
					} else {
						return moveTowards(taskToDo);
					}
				}
				// tank >fp
				else {
					if(getCurrentCell(view) instanceof FuelPump){
						if(getFuelLevel() == 100) {
							return seekFor(view, timestep);
						} else {
							return new RefuelAction();
						}
					} else {
						return moveTowards(nearestFPFromCurrent);
					}
				}
			}
			//--------------------------------------------------------------------------
			// wasteCapacity() < taskAmount

			else {
				if(MAX_WASTE < taskToDo[2]){
					// tank > task
					if(fuelLeftAfterTask > distanceBetweenTaskAndFuelPump){
						if(getCurrentCell(view) instanceof Station){
							Station currentCellStation = (Station) getCurrentCell(view);
							Task currentTask = currentCellStation.getTask();

							int newTask = tasksList.get(taskChosenIndex)[2] - getWasteCapacity();
							tasksList.get(taskChosenIndex)[2] = newTask;
							return new LoadWasteAction(currentTask);
						} else {
							return moveTowards(taskToDo);
						}
					}
					// tank > task > fp
					else {
						if(getCurrentCell(view) instanceof FuelPump){
							if(getFuelLevel() == 100) {
								return seekFor(view, timestep);
							} else {
								System.out.println("5.");
								return new RefuelAction();
							}
						} else {
							return moveTowards(nearestFPFromCurrent);
						}
					}
				}
				//------------------------------------------------------------
				else {
					// dispose action
					// may need to refuel
					//tank > well ******
					int distanceBetweenTankAndWell = Math.max(Math.abs(nearestWellFromCurrent[0] - curCoorX),Math.abs(nearestWellFromCurrent[1] - curCoorY));
					int distanceBetweenFuelPumpAndWell = Math.max(Math.abs(nearestWellFromCurrent[0] - nearestFPGoingFromWell[0]),Math.abs(nearestWellFromCurrent[1] - nearestFPGoingFromWell[1]));
					int fuelAfterWell = getFuelLevel() - distanceBetweenTankAndWell;
					// tank > well
					if(fuelAfterWell > distanceBetweenFuelPumpAndWell) {
						System.out.print("3.WELL AT:" + "(" + nearestWellFromCurrent[0] + "," + nearestWellFromCurrent[1] + ")" + " TankAt:" + "(" + curCoorX + "," + curCoorY + ")");
						if(getCurrentCell(view) instanceof Well){
							System.out.println();
							System.out.println("3.TankAt:" + "(" + curCoorX + "," + curCoorY + ") DISPOSING");
							return new DisposeWasteAction();
						} else {
							System.out.print(" GOING TO DISPOSE");
							System.out.println();
							return moveTowards(nearestWellFromCurrent);
						}
					}
					// tank goes to refuel during dispose action
					// tank > well > fp
					else {
						if(getCurrentCell(view) instanceof FuelPump){
							System.out.println("4.FP At:" + "(" + nearestFPFromCurrent[0] + "," + nearestFPFromCurrent[1] + ")" + " TankAt" + "(" + curCoorX + "," + curCoorY + ")");
							if(getFuelLevel() != 100){
								System.out.println("4.TankAt" + "(" + curCoorX + "," + curCoorY + ") REFUELLING");
								return new RefuelAction();
							}else{
								return null;
							}
						} else {
							return moveTowards(nearestFPFromCurrent);
						}
					}
				}
			}
			//----------------------------------------------------------------------
		}//---------end of if taskList.size >= 1

		// No task left tasksList
		else {
			int[] root = {0,0};
			// Find nearest fuelPump going from current tank position
			int[] nearestFuelPumpFromCurrent = allFuelPumps.get(shortestDistanceTo(allFuelPumps, new int[]{curCoorX, curCoorY}));
			int distanceBetweenRootAndCurrent = Math.max(Math.abs(root[0] - curCoorX),Math.abs(root[1] - curCoorY));

			if(getFuelLevel() > distanceBetweenRootAndCurrent){
				return seekFor(view, timestep);
			} else {
				if(getCurrentCell(view) instanceof FuelPump){
					if(getFuelLevel() == 100) {
						return seekFor(view, timestep);
					} else {
						return new RefuelAction();
					}
				} else {
					return moveTowards( nearestFuelPumpFromCurrent);
				}
			}
		}
	}

	private void moveDirection(int dir){
		switch (dir){
			case 0:
				curCoorY++;
				break;
			case 1:
				curCoorY--;
				break;
			case 2:
				curCoorX++;
				break;
			case 3:
				curCoorX--;
				break;
			case 4:
				curCoorX++;
				curCoorY++;
				break;
			case 5:
				curCoorX--;
				curCoorY++;
				break;
			case 6:
				curCoorX++;
				curCoorY--;
				break;
			case 7:
				curCoorX--;
				curCoorY--;
				break;
		}
	}
}
