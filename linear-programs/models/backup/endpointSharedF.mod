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
param NumGroups default 1;

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices := 1..NumGroups;

# FG - Set of all failure groups of size k
set FG{g in GroupIndices} within AllPairs  default {};

# MIN AND MAX PARAMS FOR NUMBER OF SRCS/DESTINATIONS
param useMinS >= 0 integer default 0;
param useMaxS >= 0 integer default card(S);
param useMinD >= 0 integer default 0;
param useMaxD >= 0 integer default card(D);

var connSurvivesFromS{s in S, g in GroupIndices} binary;
var connSurvivesToD{d in D, g in GroupIndices} binary;

# VARIABLES

# C - connection number (i) from node s to node d
var C{(s,d) in SD, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{(s,d) in SD, i in I, u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{(s,d) in SD, i in I, v in V} binary;

# Connection (s,d,i) fails because of the removal of FG[g]
var FG_Conn {(s,d) in SD, i in I, g in GroupIndices} binary;

# Connection (s,d,i) can be disconnected by the removal of any members of FG[g] - including sources and destinations
var FG_Conn_include_endpoints {(s,d) in SD, i in I, g in GroupIndices} binary;

# Sources
# At least one connection from s is disconnected by removal of FG[g]
var FG_Conn_s{s in S, g in GroupIndices} binary;

# At least one connection from s is disconnected by removal of any FG
var FG_Conn_s_any{s in S} binary;

#Destinations
# At least one connection to d is disconnected by removal of FG[g]
var FG_Conn_d{d in D, g in GroupIndices} binary;

# At least one connection to d is disconnected by removal of any FG
var FG_Conn_d_any{d in D} binary;

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {g in GroupIndices} >= 0 integer;


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

subject to minNumConnections{g in GroupIndices}:
	Num_Conns_Total >= c_total + FG_Sum[g];

# ENDPOINT CONSTRAINTS

# Source constraints
subject to minNumConnectionsNeededSource{s in S, g in GroupIndices}:
	Num_Conn_src[s] >= c_min_s[s] + sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

subject to maxNumConnectionsNeededSource{s in S, g in GroupIndices}:
	FG_Conn_s[s,g] == 1 ==> Num_Conn_src[s] <= c_max_s[s] + sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

subject to maxNumConnectionsNeededSourceNoFails{s in S}:
	FG_Conn_s_any[s] == 0 ==> Num_Conn_src[s] <= c_max_s[s];


# Destination Constraints

subject to minNumConnectionsNeededDest{d in D, g in GroupIndices}:
	Num_Conn_dst[d] >= c_min_d[d] + sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

subject to maxNumConnectionsNeededDest{d in D, g in GroupIndices}:
	FG_Conn_d[d,g] == 1 ==> Num_Conn_dst[d] <= c_max_d[d] + sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

subject to maxNumConnectionsNeededDestNoFails{d in D}:
	FG_Conn_d_any[d] == 0 ==> Num_Conn_dst[d] <= c_max_d[d];



# SRC/DEST REACHABILITY CONSTRAINTS

subject to connSurvivesFromS_1{s in S, g in GroupIndices}:
   connSurvivesFromS[s,g] <= Num_Conn_src[s] - sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

subject to connSurvivesFromS_2{s in S, g in GroupIndices}:
   connSurvivesFromS[s,g] * card(V)^4 >= Num_Conn_src[s] - sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

subject to connSurvivesToD_1{d in D, g in GroupIndices}:
   connSurvivesToD[d,g] <= Num_Conn_dst[d] - sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

subject to connSurvivesToD_2{d in D, g in GroupIndices}:
   connSurvivesToD[d,g] * card(V)^4 >= Num_Conn_dst[d] - sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

subject to minSrcsReached{g in GroupIndices}:
    sum{s in S} connSurvivesFromS[s,g] >= useMinS;

subject to minDstsReached{g in GroupIndices}:
    sum{d in D} connSurvivesToD[d,g] >= useMinD;

subject to maxSrcsReached{g in GroupIndices}:
    sum{s in S} connSurvivesFromS[s,g] <= useMaxS;

subject to maxDstsReached{g in GroupIndices}:
    sum{d in D} connSurvivesToD[d,g] <= useMaxD;

# END SRC/DEST REACHABILITY CONSTRAINTS


# Other Connection Constraints

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
subject to groupCausesConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[s,d,i,v];

subject to groupCausesConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: v != s and v != d and  (v,v) in FG[g]} NC[s,d,i,v];


# Track connections that fail due to the removal of a failure group - including the src/dest of a connection.
subject to groupCausesConnectionToFailIncludeEndpoints_1{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn_include_endpoints[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[g]} NC[s,d,i,v];

subject to groupCausesConnectionToFailIncludeEndpoints_2{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn_include_endpoints[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[g]} NC[s,d,i,v];

# Sum up the number of failed connections due to FG[g]
subject to numFailsDueToGroup{g in GroupIndices}:
	FG_Sum[g] = sum{(s,d) in SD, i in I} FG_Conn_include_endpoints[s,d,i,g];


# Put limits on the number of connections between a pair that can share a FG
subject to connectionsBetweenPairDoNotShareFG{(s,d) in SD, g in GroupIndices}:
    sum{i in I} FG_Conn[s,d,i,g] <= 1;


# Endpoints

subject to atLeastOneConnFailsForS_1{s in S, g in GroupIndices}:
    FG_Conn_s[s,g] <= sum{i in I, d in D: s != d} FG_Conn[s,d,i,g];

subject to atLeastOneConnFailsForS_2{s in S, g in GroupIndices}:
    FG_Conn_s[s,g] * card(V) ^ 4 >= sum{i in I, d in D: s != d} FG_Conn[s,d,i,g];

subject to atLeastOneConnFailsForSAny_1{s in S}:
    FG_Conn_s_any[s] <= sum{g in GroupIndices} FG_Conn_s[s,g];

subject to atLeastOneConnFailsForSAny_2{s in S}:
    FG_Conn_s_any[s] * card(V) ^ 4 >= sum{g in GroupIndices} FG_Conn_s[s,g];



subject to atLeastOneConnFailsForD_1{d in D, g in GroupIndices}:
    FG_Conn_d[d,g] <= sum{i in I, s in S: s != d} FG_Conn[s,d,i,g];

subject to atLeastOneConnFailsForD_2{d in D, g in GroupIndices}:
    FG_Conn_d[d,g] * card(V) ^ 4 >= sum{i in I, s in S: s != d} FG_Conn[s,d,i,g];

subject to atLeastOneConnFailsForDAny_1{d in D}:
    FG_Conn_d_any[d] <= sum{g in GroupIndices} FG_Conn_d[d,g];

subject to atLeastOneConnFailsForDAny_2{d in D}:
    FG_Conn_d_any[d] * card(V) ^ 4 >= sum{g in GroupIndices} FG_Conn_d[d,g];

