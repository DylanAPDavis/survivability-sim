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

# c_min - Minimum number of connections from s in S that need to survive
param c_min_s{s in S} >= 0 integer;

# c_max - Maxmimum number of connections from s in S  that need to survive
param c_max_s{s in S} >= 0 integer;

# c_min - Minimum number of connections to d in D that need to survive
param c_min_d{d in D} >= 0 integer;

# c_max - Maxmimum number of connections to d in D that need to survive
param c_max_d{d in D} >= 0 integer;

# NumGroups - Number of k-sized failure groups
param NumGroups_s{s in S};

param NumGroups_d{d in D};

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices_s{s in S} := 1..NumGroups_s[s];

set GroupIndices_d{d in D} := 1..NumGroups_d[d];

# FG - Set of all failure groups of size k
set FG_s{s in S, g in GroupIndices_s[s]} within AllPairs;

set FG_d{d in D, g in GroupIndices_d[d]} within AllPairs;


# VARIABLES

# C - connection number (i) from node s to node d
var C{(s,d) in SD, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{(s,d) in SD, i in I, u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{(s,d) in SD, i in I, v in V} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum_src {s in S, g in GroupIndices_s[s]} >= 0 integer;
var FG_Sum_dst {d in D, g in GroupIndices_d[d]} >= 0 integer;

# Connection i between (s,d) fails due to group g
var FG_Conn_src{(s,d) in SD, i in I, g in GroupIndices_s[s]} >= 0 integer;
var FG_Conn_dst{(s,d) in SD, i in I, g in GroupIndices_d[d]} >= 0 integer;	

# FG_Sum_Max - Maximum number of failed connections for a source or dest across a failure group
var FG_Sum_Max_src{s in S} >= 0 integer;
var FG_Sum_Max_dst{d in D} >= 0 integer;

# Total number of connections per (s,d) pair
var Num_Conn{(s,d) in SD} = sum{i in I} C[s,d,i];

# Number of connections from a source s
var Num_Conn_src{s in S} = sum{d in D, i in I: s != d} C[s,d,i];

# Number of connections to a destination d
var Num_Conn_dst{d in D} = sum{s in S, i in I: s != d} C[s,d,i];

# Number of connections total
var Num_Conns_Total = sum{(s,d) in SD} Num_Conn[s,d];

# Number of link usages
var Num_Links_Used = sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v];

var Total_Weight = sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v] * Weight[u,v];


# OBJECTIVE

minimize LinksUsed:
	Num_Links_Used;

minimize Connections:
	Num_Conns_Total;

minimize TotalCost:
	Total_Weight;

## Connection Constraints


subject to minNumConnectionsSources:
	sum{s1 in S} Num_Conn_src[s1] >= c_total + sum{s in S} FG_Sum_Max_src[s];

subject to minNumConnectionsDestinations:
	sum{d1 in D} Num_Conn_dst[d1] >= c_total +  sum{d in D} FG_Sum_Max_dst[d];

subject to minNumConnectionsNeededSource{s in S}:
	Num_Conn_src[s] >= c_min_s[s] + FG_Sum_Max_src[s];

subject to maxNumConnectionsNeededSource{s in S}:
	Num_Conn_src[s] <= c_max_s[s] + FG_Sum_Max_src[s];

subject to minNumConnectionsNeededDest{d in D}:
	Num_Conn_dst[d] >= c_min_d[d] + FG_Sum_Max_dst[d];

subject to maxNumConnectionsNeededDest{d in D}:
	Num_Conn_dst[d] <= c_max_d[d] + FG_Sum_Max_dst[d];

subject to noSelfConnections{(s,d) in SD: s == d}:
	Num_Conn[s,d] = 0;

subject to oneFlowFromNodeInConn{(s,d) in SD, i in I, u in V}:
	sum{v in V} L[s,d,i,u,v] <= 1;

subject to oneFlowIntoNodeInConn{(s,d) in SD, i in I, v in V}:
	sum{u in V} L[s,d,i,u,v] <= 1;

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

# Node is in a connection
subject to nodeInConnection_A{(s,d) in SD, i in I, v in V}:
	NC[s,d,i,v] <= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

subject to nodeInConnection_B{(s,d) in SD, i in I, v in V}:
	NC[s,d,i,v] * card(V)^4 >= sum{u in V} L[s,d,i,u,v] + sum{w in V} L[s,d,i,v,w];

## Failure Constraints

subject to groupCausesSrcConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices_s[s]}:
	FG_Conn_src[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG_s[s,g] or (v,u) in FG_s[s,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG_s[s,g]} NC[s,d,i,v];

subject to groupCausesSrcConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices_s[s]}:
	FG_Conn_src[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG_s[s,g] or (v,u) in FG_s[s,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG_s[s,g]} NC[s,d,i,v];

subject to groupCausesDstConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices_d[d]}:
	FG_Conn_dst[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG_d[d,g] or (v,u) in FG_d[d,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG_d[d,g]} NC[s,d,i,v];

subject to groupCausesDstConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices_d[d]}:
	FG_Conn_dst[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG_d[d,g] or (v,u) in FG_d[d,g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG_d[d,g]} NC[s,d,i,v];


# Sum up the failureSet per failure group
subject to totalFailuresPerGroupSrc{s in S, g in GroupIndices_s[s]}:
	FG_Sum_src[s,g] = sum{d in D, i in I: s != d} FG_Conn_src[s,d,i,g];

# Sum up the failureSet per failure group
subject to totalFailuresPerGroupDst{d in D, g in GroupIndices_d[d]}:
	FG_Sum_dst[d,g] = sum{s in S, i in I: s != d} FG_Conn_dst[s,d,i,g];

subject to maxFailuresFromGroupSrc{s in S, g in GroupIndices_s[s]}:
	FG_Sum_Max_src[s] >= FG_Sum_src[s,g];

subject to maxFailuresFromGroupDst{d in D, g in GroupIndices_d[d]}:
	FG_Sum_Max_dst[d] >= FG_Sum_dst[d,g];
