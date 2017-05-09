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

# S - Sources
set S;	

# D - Destinations
set D;

# SD - Source/Dest pairs
set SD within {S cross D} default {s in S, d in D};

# c_total - Total number of connections needed after k failures
param c_total >= 0 integer;

# c_min - Minimum number of connections per (s,d) pair that need to survive k failures
param c_min_sd{SD} >= 0 integer;

# c_max - Maxmimum number of connections per (s,d) pair that need to survive k failures
param c_max_sd{SD} >= 0 integer;

# F[s,d] - Failure Matrix - Sets of potential failure nodes/links per (s,d) pair
set F{SD} within AllPairs;

# NumGroups - Number of k-sized failure groups
param NumGroups{SD};

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices{(s,d) in SD} := 1..NumGroups[s,d];

# FG - Set of all failure groups of size k
set FG {(s,d) in SD, g in GroupIndices[s,d]} within AllPairs;

param num_fails_allowed >= 0 integer;


# VARIABLES

# C - connection number (i) from node s to node d
var C{(s,d) in SD, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{(s,d) in SD, i in I, u in V, v in V} binary;

# FC - Number of failed connections caused by node v / link u, v
var FC{(s,d) in SD, u in V, v in V} >= 0 integer;

# NC - Node v is in Connection (s,d,i)
var NC{(s,d) in SD, i in I, v in V} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {(s,d) in SD, g in GroupIndices[s,d]} >= 0 integer;

# Number of connections
var Num_Conn{(s,d) in SD} = sum{i in I} C[s,d,i];

# Number of link usages
var Num_Link_Usages = sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v];

# Connection i between (s,d) fails
var Conn_Fails_Pair{(s,d) in SD, i in I} binary;

# Total number of failed connections between (s,d) pair
var Total_F_Pair{(s,d) in SD} = sum{i in I} Conn_Fails_Pair[s,d,i];

# Total Fails across all pairs
var Total_F = sum{(s,d) in SD} Total_F_Pair[s,d];

# Total number of Connections
var Total_C = sum{(s,d) in SD} Num_Conn[s,d];


# OBJECTIVE

minimize Objective:
	Num_Link_Usages;

## Connection Constraints

subject to totalConnectionsNeeded{if Total_F < num_fails_allowed}:
	Total_C >= c_total + Total_F;

subject to totalConnectionsNeeded2{if Total_F >= num_fails_allowed}:
	Total_C >= c_total + num_fails_allowed;

subject to minNumConnectionsNeeded{(s,d) in SD, g in GroupIndices[s,d]}:
	Num_Conn[s,d] >= c_min_sd[s,d] + FG_Sum[s,d,g];

subject to maxNumConnectionsNeeded{(s,d) in SD, g in GroupIndices[s,d]}:
	Num_Conn[s,d] <= c_max_sd[s,d] + FG_Sum[s,d,g];

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

subject to noReverseFlowIfForward{(s,d) in SD, i in I, u in V, v in V}:
	L[s,d,i,u,v] + L[s,d,i,v,u] <= 1;

# Node is in a connection
subject to nodeInConnection_A{(s,d) in SD, i in I, v in V}:
	NC[s,d,i,v] <= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

subject to nodeInConnection_B{(s,d) in SD, i in I, v in V}:
	NC[s,d,i,v] * card(V)^4 >= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

## Failure Constraints

# Number of failures caused by a link --> Number of connections that include that element
subject to numFailedConnectionsLink{(s,d) in SD, u in V, v in V :u != v and ((u,v) in F[s,d] or (v,u) in F[s,d])}:
	FC[s,d,u,v] = sum{i in I} L[s,d,i,u,v];

# Number of failures caused by a node
subject to numFailedConnectionsNode{(s,d) in SD, v in V: (v,v) in F[s,d]}:
	FC[s,d,v,v] = sum{i in I} NC[s,d,i,v];

# Sum up the failures per failure group
subject to totalFailuresPerGroup{(s,d) in SD, g in GroupIndices[s,d]}:
	FG_Sum[s,d,g] = sum{u in V, v in V: (u,v) in FG[s,d,g] or (v,u) in FG[s,d,g]} FC[s,d,u,v];

subject to connectionFailedIfAtLeastOneFailure{(s,d) in SD, i in I}:
	if sum{u in V, v in V: u != v and ((u,v) in F[s,d] or (v,u) in F[s,d])} L[s,d,i,u,v] > 0 or sum{v in V: (v,v) in F[s,d]} NC[s,d,i,v] > 0 then F_Total_Pair[s,d] = 1;

