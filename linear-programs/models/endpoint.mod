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

# c_min - Minimum number of connections from s in S that need to survive
param c_min_s{s in S} >= 0 integer;

# c_max - Maxmimum number of connections from s in S  that need to survive
param c_max_s{s in S} >= 0 integer;

# c_min - Minimum number of connections to d in D that need to survive
param c_min_d{d in D} >= 0 integer;

# c_max - Maxmimum number of connections to d in D that need to survive
param c_max_d{d in D} >= 0 integer;

# F_s - Failure Matrix - Sets of potential failure nodes/links for source s
set F{v in V : v in S or v in D} within AllPairs;

# NumGroups - Number of k-sized failure groups
param NumGroups{v in V : v in S or v in D};

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices{v in V : v in S or v in D} := 1..NumGroups[v];

# FG - Set of all failure groups of size k
set FG {v in V, g in GroupIndices[v] : v in S or v in D} within AllPairs;


# VARIABLES

# C - connection number (i) from node s to node d
var C{s in V, d in V, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{s in V, d in V, i in I, u in V, v in V} binary;

# FC - Number of failed connections caused by node v / link u, v
var FC{s in V, u in V, v in V} >= 0 integer;

# NC - Node v is in Connection (s,d,i)
var NC{s in V, d in V, i in I, v in V} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {v in V, g in GroupIndices[v] : v in S or v in D} >= 0 integer;

var FG_Conn_src{s in S, d in D, i in I, g in GroupIndices[s]} >= 0 integer;

var FG_Conn_dst{s in S, d in D, i in I, g in GroupIndices[d]} >= 0 integer;	

# Total number of connections per (s,d) pair
var Num_Conn{s in S, d in D} = sum{i in I} C[s,d,i];

# Number of connections from a source s
var Num_Conn_s{s in S} = sum{d in D, i in I} C[s,d,i];

# Number of connections to a destination d
var Num_Conn_d{d in D} = sum{s in S, i in I} C[s,d,i];

# Number of link usages
var Num_Link_Usages = sum{s in V, d in V, i in I, u in V, v in V} L[s,d,i,u,v];


# OBJECTIVE

minimize Objective:
	Num_Link_Usages;

## Connection Constraints

subject to minNumConnectionsSources:
	sum{s in S Num_Conn_s[s] >= c_total + sum{s in S} max{g in GroupIndices[s]} FG_Sum[s,g];

subject to minNumConnectionsDestinations:
	sum{d in D Num_Conn_d[d] >= c_total + sum{d in D} max{g in GroupIndices[d]} FG_Sum[d,g];

subject to minNumConnectionsNeededSource{s in S, g in GroupIndices[s]}:
	Num_Conn_s[s] >= c_min_s[s] + FG_Sum[s,g];

subject to maxNumConnectionsNeededSource{s in S, g in GroupIndices[s]}:
	Num_Conn_s[s] <= c_max_s[s] + FG_Sum[s,g];

subject to minNumConnectionsNeededDest{d in D, g in GroupIndices[d]}:
	Num_Conn_d[d] >= c_min_d[d] + FG_Sum[d,g];

subject to maxNumConnectionsNeededDest{d in D, g in GroupIndices[d]}:
	Num_Conn_d[d] <= c_max_d[d] + FG_Sum[d,g];

subject to noSelfConnections{(s,d) in SD: s == d}:
	Num_Conn[s,d] = 0;

subject to flowOnlyIfConnectionAndLinkExists{s in V, d in V, i in I, u in V, v in V}:
	L[s,d,i,u,v] <= A[u,v] * C[s,d,i];

subject to intermediateFlow{s in V, d in V, i in I, v in V: v != s and v != d}:
	sum{u in V} L[s,d,i,u,v] - sum{w in V} L[s,d,i,v,w] = 0;

subject to sourceFlow{s in V, d in V, i in I}:
	sum{u in V} L[s,d,i,u,s] - sum{w in V} L[s,d,i,s,w] = -1 * C[s,d,i];

subject to destinationFlow{s in V, d in V, i in I}:
	sum{u in V} L[s,d,i,u,d] - sum{w in V} L[s,d,i,d,w] = C[s,d,i];	

subject to flowOnlyInConnection{s in V, d in V, i in I, u in V, v in V}:
	L[s,d,i,u,v] <= C[s,d,i];

subject to noFlowIntoSource{s in V, d in V, i in I, u in V}:
	L[s,d,i,u,s] = 0;

# Node is in a connection
subject to nodeInConnection_A{s in V, d in V, i in I, v in V: (s,d) in SD or (d,s) in SD}:
	NC[s,d,i,v] <= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

subject to nodeInConnection_B{s in V, d in V, i in I, v in V: (s,d) in SD or (d,s) in SD}:
	NC[s,d,i,v] * card(V)^4 >= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

## Failure Constraints

subject to groupCausesSrcConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices[s]}:
	FG_Conn_Src[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[s,g] or (v,u) in FG[s,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[s,g]} NC[s,d,i,v];

subject to groupCausesSrcConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices[s]}:
	FG_Conn_Src[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[s,g] or (v,u) in FG[s,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[s,g]} NC[s,d,i,v];

subject to groupCausesDstConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices[d]}:
	FG_Conn_Dst[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[d,g] or (v,u) in FG[d,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[d,g]} NC[s,d,i,v];

subject to groupCausesDstConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices[d]}:
	FG_Conn_Dst[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[d,g] or (v,u) in FG[d,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[d,g]} NC[s,d,i,v];


# Sum up the failureSet per failure group
subject to totalFailuresPerGroupSrc{s in S, g in GroupIndices[s]}:
	FG_Sum[s,g] = sum{d in D, i in I} FG_Conn_Src[s,d,i,g];

# Sum up the failureSet per failure group
subject to totalFailuresPerGroupDst{d in D, g in GroupIndices[d]}:
	FG_Sum[d,g] = sum{S in S, i in I} FG_Conn_Dst[s,d,i,g];
