# GIVEN

# Set of Vertices
set V;

# All pairs of nodes
set AllPairs = {V,V};

# Maximum Number of Connections
param I_max >= 0 integer;

# Set of Connection Indices
set I := 1..I_max;

# Physical Links - 1 if Link exists between u and v
param A{u in V, v in V} binary default 0;

# Link cost
param Weight{u in V, v in V} integer default 0;

set S;
set D;

# S - Sources
param s symbolic in S;

# D - Destinations
param d symbolic in D;

# c_total - Total number of connections needed after k failures
param c_total >= 0 integer default 1;

# NumGroups - Number of k-sized failure groups
param NumGroups default 1;

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices := 1..NumGroups;

# FG - Set of all failure groups of size k
set FG {g in GroupIndices} within AllPairs default {};

# Traffic combination
param combineSourceTraffic binary default 0;
param combineDestTraffic binary default 0;

# VARIABLES

# C - connection number (i) from node s to node d
var C{i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{s1 in S, d1 in D, i in I, u in V, v in V} binary;

# There is at least one link flow between pair (s,d)
var L_sd{u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{i in I, v in V} binary;

# Connection (s,d,i) fails because of the removal of FG[g]. Excludes the source and destination
var FG_Conn {i in I, g in GroupIndices} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {g in GroupIndices} >= 0 integer;

# Number of connections
var Num_Conn = sum{i in I} C[i];


# OBJECTIVE VARIABLES

# Number of connections total
var Num_Conns_Total = Num_Conn;

# Number of link usages
var Num_Links_Used >= 0 integer;

# Total weight of all used links
var Total_Weight >= 0 integer;


# OBJECTIVE


minimize linksused:
	Num_Links_Used;

minimize connections:
	Num_Conns_Total;

minimize totalcost:
	Total_Weight;


# Objective definition constraints
# LINKS USED
subject to linksUsed_doNotCombineTraffic:
    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{i in I, u in V, v in V} L[s,d,i,u,v];

subject to linksUsed_combineBothTraffic:
    combineSourceTraffic == 1 or combineDestTraffic == 1 ==> Num_Links_Used >= sum{u in V, v in V} L_sd[u,v];

# TOTAL WEIGHT
subject to totalWeight_doNotCombineTraffic:
    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Total_Weight >= sum{i in I, u in V, v in V} L[s,d,i,u,v] * Weight[u,v];

subject to totalWeight_combineBothTraffic:
    combineSourceTraffic == 1 or combineDestTraffic == 1 ==> Total_Weight >= sum{u in V, v in V} L_sd[u,v] * Weight[u,v];

## Connection Constraints

subject to totalConnectionsNeeded{g in GroupIndices}:
	Num_Conns_Total >= c_total + FG_Sum[g];

subject to noSelfConnections:
	s == d ==> Num_Conn = 0;

subject to flowOnlyIfConnectionAndLinkExists{ i in I, u in V, v in V}:
	L[s,d,i,u,v] <= A[u,v] * C[i];

subject to intermediateFlow{i in I, v in V: v != s and v != d}:
	sum{u in V} L[s,d,i,u,v] - sum{w in V} L[s,d,i,v,w] = 0;

subject to sourceFlow{i in I}:
	sum{u in V} L[s,d,i,u,s] - sum{w in V} L[s,d,i,s,w] = -1 * C[i];

subject to destinationFlow{ i in I}:
	sum{u in V} L[s,d,i,u,d] - sum{w in V} L[s,d,i,d,w] = C[i];

subject to flowOnlyInConnection{i in I, u in V, v in V}:
	L[s,d,i,u,v] <= C[i];

subject to noFlowIntoSource{i in I, u in V}:
	L[s,d,i,u,s] = 0;

subject to oneFlowFromNodeInConn{i in I, u in V}:
	sum{v in V} L[s,d,i,u,v] <= 1;

subject to oneFlowIntoNodeInConn{i in I, v in V}:
	sum{u in V} L[s,d,i,u,v] <= 1;

subject to noReverseFlowIfForward{i in I, u in V, v in V}:
	L[s,d,i,u,v] + L[s,d,i,v,u] <= 1;

# Node is in a connection
subject to nodeInConnection_A{i in I, v in V}:
	NC[i,v] <= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

subject to nodeInConnection_B{i in I, v in V}:
	NC[i,v] * card(V)^4 >= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];


### L_sd definition constraints

subject to flowOnLinkBetweenPair_A{ u in V, v in V}:
	L_sd[u,v] <= sum{i in I} L[s,d,i,u,v];

subject to flowOnLinkBetweenPair_B{ u in V, v in V}:
	L_sd[u,v] * card(V)^4 >= sum{i in I} L[s,d,i,u,v];


## Failure Constraints

# Connection (s,d,i) fails or does not fail due to FG[g]

# Number of failures caused by a link --> Number of connections that include that element. Exclude the src/dest of a connection.
subject to groupCausesConnectionToFail_1{i in I, g in GroupIndices}:
	FG_Conn[i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[i,v];

subject to groupCausesConnectionToFail_2{ i in I, g in GroupIndices}:
	FG_Conn[i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[i,v];

# Sum up the number of failed connections due to FG[g]
subject to numFailsDueToGroup{g in GroupIndices}:
	FG_Sum[g] = sum{i in I} FG_Conn[i,g];

# Put limits on the number of connections between a pair  that can share a FG
subject to connectionsBetweenPairDoNotShareFG{g in GroupIndices}:
    sum{i in I} FG_Conn[i,g] <= 1;

