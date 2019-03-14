# Designing-Intelligent-agents
JADE (Java Agent DEvelopment ) simulates a waste collection autonomous robot hybrid architecture..
©2017 Brian Logan. A simple AI agent developed in Java. Using a pre-developed environment developed by professor Brian Logan.

Requirements
The problem consists of a 2D environment, in which a single agent must collect and dispose of waste (CO2) from stations, e.g., carbon capture and storage. Stations periodically generate tasks – requests to dispose of a specified amount of waste. The environment also contains a number of wells where waste can be deposited. The goal of the agent is to dispose of as much waste as possible in a fixed period of time.

Task Environment
The standard task environment is defined as:

• the environment is an infinite 2D grid that contains randomly distributed stations, wells and refuelling points

• stations periodically generate tasks – requests to dispose of a specified amount of waste

• tasks persist until they are achieved (a station has at most one task at any time)

• the maximum amount of waste that must be disposed of in a single task is 5,000 litres

• wells can accept an infinite amount of waste

• refuelling points contain an infinite amount of fuel

• in each run, there is always a refuelling station in the centre of the environment

• a run lasts 10,000 timesteps

• an agent can sense only its current position (which may be a station, well or refuelling point)

• the agent can take waste from a station and dispose of it in a well

• moving around the environment requires fuel, which the agent must replenish at a fuel station

• the agent can carry a maximum of 100 litres of fuel and 1,000 litres of waste

• the agent starts out in the centre of the environment (at the fuel station) with 100 litres of fuel and no waste

• the agent moves at 1 cell / timestep and consumes 1 litre of fuel / cell

• filling the fuel and waste tanks and delivering waste to a well takes one timestep

• if the agent runs out of fuel, it can do nothing for the rest of the run

• the success (score) of an agent in the task environment is determined by the amount of waste delivered

The task environment should not be modified or extended. All other decisions regarding software design and implementation strategy are up to you. However you will be given guidance and feedback on your project in individual tutorials. You must implement an agent that completes the task in the specified task environment, and must include in your final report an evaluation of the performance of your agent (average score over at least ten runs).
