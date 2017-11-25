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
param s;

# D - Destinations
set D;

# c_total - Total number of connections needed after k failures
param c_total >= 0 integer default card(SD);

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
var C{d in D, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{d in D, i in I, u in V, v in V} binary;

# There is at least one link flow going from source s
var L_s{u in V, v in V} binary;

# There is at least one link flow going to destination d
var L_d{d in D, u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{d in D, i in I, v in V} binary;

# Connection (s,d,i) fails because of the removal of FG[g]. Excludes the source and destination
var FG_Conn {d in D, i in I, g in GroupIndices} binary;

# Connection (s,d,i) can be disconnected by the removal of any members of FG[g] - including sources and destinations
var FG_Conn_include_endpoints {d in D, i in I, g in GroupIndices} binary;


#Destinations
# At least one connection to d is disconnected by removal of FG[g]
var FG_Conn_d{d in D, g in GroupIndices} binary;

# At least one connection to d is disconnected by removal of any FG
var FG_Conn_d_any{d in D} binary;

var connSurvivesToD{d in D, g in GroupIndices} binary;

# END INDICATOR VARIABLES

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {g in GroupIndices} >= 0 integer;

# Number of connections to a destination d
var Num_Conn_dst{d in D} = sum{i in I} C[d,i];
# END ENDPOINT VARIABLES


# OBJECTIVE VARIABLES

# Number of connections total
var Num_Conns_Total = sum{d in D} Num_Conn_dst[d];

# Number of link usages
var Num_Links_Used >= 0 integer;

# Total weight of all used links
var Total_Weight >= 0 integer;


# OBJECTIVE

minimize LinksUsed:
	Num_Links_Used;

minimize Connections:
	Num_Conns_Total;

minimize TotalCost:
	Total_Weight;


# Objective definition constraints
# LINKS USED
subject to linksUsed_combineTraffic_SourceOnly:
    combineSourceTraffic == 1 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{u in V, v in V} L_s[u,v];

subject to linksUsed_combineTraffic_DestOnly:
    combineSourceTraffic == 0 and combineDestTraffic == 1 ==> Num_Links_Used >= sum{d in D, u in V, v in V} L_d[d,u,v];

subject to linksUsed_doNotCombineTraffic:
    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{d in D, i in I, u in V, v in V} L[d,i,u,v];

subject to linksUsed_combineBothTraffic:
    combineSourceTraffic == 1 and combineDestTraffic == 1 ==> Num_Links_Used >= sum{u in V, v in V} L_s[u,v];

# TOTAL WEIGHT
subject to totalWeight_combineTraffic_SourceOnly:
    combineSourceTraffic == 1 and combineDestTraffic == 0 ==> Total_Weight >= sum{u in V, v in V} L_s[u,v] * Weight[u,v];

subject to totalWeight_combineTraffic_DestOnly:
    combineSourceTraffic == 0 and combineDestTraffic == 1 ==> Total_Weight >= sum{d in D, u in V, v in V} L_d[d,u,v] * Weight[u,v];

subject to totalWeight_doNotCombineTraffic:
    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Total_Weight >= sum{d in D, i in I, u in V, v in V} L[d,i,u,v] * Weight[u,v];

subject to totalWeight_combineBothTraffic:
    combineSourceTraffic == 1 and combineDestTraffic == 1 ==> Total_Weight >= sum{u in V, v in V} L_s[u,v] * Weight[u,v];

## Connection Constraints

subject to totalConnectionsNeeded{g in GroupIndices}:
	Num_Conns_Total >= c_total + FG_Sum[g];

subject to noSelfConnections{d in D}:
	s == d ==> Num_Conn_dst[d] = 0;

subject to minNumConnectionsNeededDest{d in D, g in GroupIndices}:
	Num_Conn_dst[d] >= 0 + sum{i in I: s != d} FG_Conn[d,i,g];

subject to maxNumConnectionsNeededDest{d in D, g in GroupIndices}:
	FG_Conn_d[d,g] == 1 ==> Num_Conn_dst[d] <= 1 + sum{i in I: s != d} FG_Conn[d,i,g];

subject to maxNumConnectionsNeededDestNoFails{d in D}:
	FG_Conn_d_any[d] == 0 ==> Num_Conn_dst[d] <= 1;


subject to connSurvivesToD_1{d in D, g in GroupIndices}:
   connSurvivesToD[d,g] <= Num_Conn_dst[d] - sum{i in I: s != d} FG_Conn[d,i,g];

subject to connSurvivesToD_2{d in D, g in GroupIndices}:
   connSurvivesToD[d,g] * card(V)^4 >= Num_Conn_dst[d] - sum{i in I: s != d} FG_Conn[d,i,g];

subject to minDstsReached{g in GroupIndices}:
    sum{d in D} connSurvivesToD[d,g] >= 1;

subject to maxDstsReached{g in GroupIndices}:
    sum{d in D} connSurvivesToD[d,g] <= 1;




subject to flowOnlyIfConnectionAndLinkExists{d in D, i in I, u in V, v in V}:
	L[d,i,u,v] <= A[u,v] * C[d,i];

subject to intermediateFlow{d in D, i in I, v in V: v != s and v != d}:
	sum{u in V} L[d,i,u,v] - sum{w in V} L[d,i,v,w] = 0;

subject to sourceFlow{d in D, i in I}:
	sum{u in V} L[d,i,u,s] - sum{w in V} L[d,i,s,w] = -1 * C[d,i];

subject to destinationFlow{d in D, i in I}:
	sum{u in V} L[d,i,u,d] - sum{w in V} L[d,i,d,w] = C[d,i];

subject to flowOnlyInConnection{d in D, i in I, u in V, v in V}:
	L[d,i,u,v] <= C[d,i];

subject to noFlowIntoSource{d in D, i in I, u in V}:
	L[d,i,u,s] = 0;

subject to oneFlowFromNodeInConn{d in D, i in I, u in V}:
	sum{v in V} L[d,i,u,v] <= 1;

subject to oneFlowIntoNodeInConn{d in D, i in I, v in V}:
	sum{u in V} L[d,i,u,v] <= 1;

subject to noReverseFlowIfForward{d in D, i in I, u in V, v in V}:
	L[d,i,u,v] + L[d,i,v,u] <= 1;

# Node is in a connection
subject to nodeInConnection_A{d in D, i in I, v in V}:
	NC[d,i,v] <= sum{u in V} L[d,i,u,v] + sum{w in V} L[d,i,v,w];

subject to nodeInConnection_B{d in D, i in I, v in V}:
	NC[d,i,v] * card(V)^4 >= sum{u in V} L[d,i,u,v] + sum{w in V} L[d,i,v,w];


### L_sd definition constraints

subject to flowOnLinkToDest_A{d in D, u in V, v in V}:
	L_d[d,u,v] <= sum{i in I} L[d,i,u,v];

subject to flowOnLinkToDest_B{d in D, u in V, v in V}:
	L_d[d,u,v] * card(V)^4 >= sum{i in I} L[d,i,u,v];

subject to flowOnLinkFromSrc_A{u in V, v in V}:
	L_s[u,v] <= sum{d in D, i in I} L[d,i,u,v];

subject to flowOnLinkFromSrc_B{u in V, v in V}:
	L_s[u,v] * card(V)^4 >= sum{d in D, i in I} L[d,i,u,v];


## Failure Constraints

# Connection (s,d,i) fails or does not fail due to FG[g]

# Number of failures caused by a link --> Number of connections that include that element. Exclude the src/dest of a connection.
subject to groupCausesConnectionToFail_1{d in D, i in I, g in GroupIndices}:
	FG_Conn[d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[d,i,v];

subject to groupCausesConnectionToFail_2{d in D, i in I, g in GroupIndices}:
	FG_Conn[d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[d,i,v];

subject to atLeastOneConnFailsForD_1{d in D, g in GroupIndices}:
    FG_Conn_d[d,g] <= sum{i in I: s != d} FG_Conn[d,i,g];

subject to atLeastOneConnFailsForD_2{d in D, g in GroupIndices}:
    FG_Conn_d[d,g] * card(V) ^ 4 >= sum{i in I: s != d} FG_Conn[d,i,g];

subject to atLeastOneConnFailsForDAny_1{d in D}:
    FG_Conn_d_any[d] <= sum{g in GroupIndices} FG_Conn_d[d,g];

subject to atLeastOneConnFailsForDAny_2{d in D}:
    FG_Conn_d_any[d] * card(V) ^ 4 >= sum{g in GroupIndices} FG_Conn_d[d,g];

# Track connections that fail due to the removal of a failure group - including the src/dest of a connection.
subject to groupCausesConnectionToFailIncludeEndpoints_1{d in D, i in I, g in GroupIndices}:
	FG_Conn_include_endpoints[d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[d,i,u,v] + sum{v in V: (v,v) in FG[g]} NC[d,i,v];

subject to groupCausesConnectionToFailIncludeEndpoints_2{d in D, i in I, g in GroupIndices}:
	FG_Conn_include_endpoints[d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[d,i,u,v] + sum{v in V: (v,v) in FG[g]} NC[d,i,v];

# Sum up the number of failed connections due to FG[g]
subject to numFailsDueToGroup{g in GroupIndices}:
	FG_Sum[g] = sum{d in D, i in I} FG_Conn_include_endpoints[d,i,g];

# Put limits on the number of connections between a pair  that can share a FG
subject to connectionsBetweenPairDoNotShareFG{d in D, g in GroupIndices}:
    sum{i in I} FG_Conn[d,i,g] <= 1;

