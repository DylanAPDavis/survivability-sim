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

# S - Sources
set S;	

# D - Destinations
set D;

# SD - Source/Dest pairs
set SD within {S cross D} default {s in S, d in D: s != d};

# c_total - Total number of connections needed after k failures
param c_total >= 0 integer;

# c_min - Minimum number of connections per (s,d) pair that need to survive k failures
param c_min_sd{SD} >= 0 integer;

# c_max - Maxmimum number of connections per (s,d) pair that need to survive k failures
param c_max_sd{SD} >= 0 integer;

# NumGroups - Number of k-sized failure groups
param NumGroups{SD} default 1;

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices{(s,d) in SD} := 1..NumGroups[s,d];

# FG - Set of all failure groups of size k
set FG {(s,d) in SD, g in GroupIndices[s,d]} within AllPairs default {};

# VARIABLES

# C - connection number (i) from node s to node d
var C{(s,d) in SD, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{(s,d) in SD, i in I, u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{(s,d) in SD, i in I, v in V} binary;

# Connection (s,d,i) fails because of the removal of FG[s,d,g]
var FG_Conn {(s,d) in SD, i in I, g in GroupIndices[s,d]} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {(s,d) in SD, g in GroupIndices[s,d]} >= 0 integer;

# FG_Sum_Max - Maximum number of failed connections for a (s,d) pair across a failure group
var FG_Sum_Max{(s,d) in SD} >= 0 integer;

# Number of connections
var Num_Conn{(s,d) in SD} = sum{i in I} C[s,d,i];

# Number of connections total
var Num_Conns_Total = sum{(s,d) in SD} Num_Conn[s,d];

# Number of link usages
var Num_Links_Used = sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v];

# Total weight of all used links
var Total_Weight = sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v] * Weight[u,v];


# OBJECTIVE

minimize LinksUsed:
	Num_Links_Used;

minimize Connections:
	Num_Conns_Total;

minimize TotalCost:
	Total_Weight;

## Connection Constraints

subject to totalNumConnections:
	Num_Conns_Total >= c_total + sum{(s,d) in SD} FG_Sum_Max[s,d];

subject to minNumConnectionsNeeded{(s,d) in SD}:
	Num_Conn[s,d] >= c_min_sd[s,d] + FG_Sum_Max[s,d];

subject to maxNumConnectionsNeeded{(s,d) in SD}:
	Num_Conn[s,d] <= c_max_sd[s,d] + FG_Sum_Max[s,d];

subject to noSelfConnections{(s,d) in SD: s == d}:
	Num_Conn[s,d] = 0;

subject to flowOnlyIfConnectionAndLinkExists{(s,d) in SD, i in I, u in V, v in V}:
	L[s,d,i,u,v] <= A[u,v] * C[s,d,i];

subject to intermediateFlow{(s,d) in SD, i in I, v in V: v != s and v != d}:
	sum{u in V} L[s,d,i,u,v] - sum{w in V} L[s,d,i,v,w] = 0;

subject to sourceFlow{(s,d) in SD, i in I}:
	sum{u in V} L[s,d,i,u,s] - sum{w in V} L[s,d,i,s,w] = -1 * C[s,d,i];

subject to destinationFlow{(s,d) in SD, i in I}:
	sum{u in V} L[s,d,i,u,d] - sum{w in V} L[s,d,i,d,w] = C[s,d,i];	

subject to flowOnlyInConnection{(s,d) in SD, i in I, u in V, v in V}:
	L[s,d,i,u,v] <= C[s,d,i];

subject to noFlowIntoSource{(s,d) in SD, i in I, u in V}:
	L[s,d,i,u,s] = 0;

subject to oneFlowFromNodeInConn{(s,d) in SD, i in I, u in V}:
	sum{v in V} L[s,d,i,u,v] <= 1;

subject to oneFlowIntoNodeInConn{(s,d) in SD, i in I, v in V}:
	sum{u in V} L[s,d,i,u,v] <= 1;

subject to noReverseFlowIfForward{(s,d) in SD, i in I, u in V, v in V}:
	L[s,d,i,u,v] + L[s,d,i,v,u] <= 1;

# Node is in a connection
subject to nodeInConnection_A{(s,d) in SD, i in I, v in V}:
	NC[s,d,i,v] <= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

subject to nodeInConnection_B{(s,d) in SD, i in I, v in V}:
	NC[s,d,i,v] * card(V)^4 >= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];


## Failure Constraints

# Connection (s,d,i) fails or does not fail due to FG[s,d,g]

subject to groupCausesConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices[s,d]}:
	FG_Conn[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[s,d,g] or (v,u) in FG[s,d,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[s,d,g]} NC[s,d,i,v];

subject to groupCausesConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices[s,d]}:
	FG_Conn[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[s,d,g] or (v,u) in FG[s,d,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[s,d,g]} NC[s,d,i,v];

# Sum up the number of failed connections due to FG[s,d,g]
subject to numFailsDueToGroup{(s,d) in SD, g in GroupIndices[s,d]}:
	FG_Sum[s,d,g] = sum{i in I} FG_Conn[s,d,i,g];

subject to maxFailuresFromGroup{(s,d) in SD, g in GroupIndices[s,d]}:
	FG_Sum_Max[s,d] >= FG_Sum[s,d,g];


#-------------------------------------------------------
