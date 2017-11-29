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
param d;

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

param useMinS default 1;
param useMaxS default 1;

# VARIABLES

# C - connection number (i) from node s to node d
var C{s in S, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{s in S, i in I, u in V, v in V} binary;

# There is at least one link flow going from source s
var L_s{s in S, u in V, v in V} binary;

# There is at least one link flow going to destination d
var L_d{u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{s in S, i in I, v in V} binary;

# Connection (s,d,i) fails because of the removal of FG[g]. Excludes the source and destination
var FG_Conn {s in S, i in I, g in GroupIndices} binary;

# Connection (s,d,i) can be disconnected by the removal of any members of FG[g] - including sources and destinations
var FG_Conn_include_endpoints {s in S, i in I, g in GroupIndices} binary;


#Destinations
# At least one connection from s is disconnected by removal of FG[g]
var FG_Conn_s{s in S, g in GroupIndices} binary;

# At least one connection from s is disconnected by removal of any FG
var FG_Conn_s_any{s in S} binary;

var connSurvivesFromS{s in S, g in GroupIndices} binary;

# END INDICATOR VARIABLES

# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {g in GroupIndices} >= 0 integer;

# Number of connections from a source s
var Num_Conn_src{s in S} = sum{i in I} C[s,i];
# END ENDPOINT VARIABLES


# OBJECTIVE VARIABLES

# Number of connections total
var Num_Conns_Total = sum{s in S} Num_Conn_src[s];

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
    combineSourceTraffic == 1 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{s in S, u in V, v in V} L_s[s,u,v];

subject to linksUsed_combineTraffic_DestOnly:
    combineSourceTraffic == 0 and combineDestTraffic == 1 ==> Num_Links_Used >= sum{u in V, v in V} L_d[u,v];

subject to linksUsed_doNotCombineTraffic:
    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{s in S, i in I, u in V, v in V} L[s,i,u,v];

subject to linksUsed_combineBothTraffic:
    combineSourceTraffic == 1 and combineDestTraffic == 1 ==> Num_Links_Used >= sum{s in S, u in V, v in V} L_s[s,u,v];

# TOTAL WEIGHT
subject to totalWeight_combineTraffic_SourceOnly:
    combineSourceTraffic == 1 and combineDestTraffic == 0 ==> Total_Weight >= sum{s in S, u in V, v in V} L_s[s,u,v] * Weight[u,v];

subject to totalWeight_combineTraffic_DestOnly:
    combineSourceTraffic == 0 and combineDestTraffic == 1 ==> Total_Weight >= sum{u in V, v in V} L_d[u,v] * Weight[u,v];

subject to totalWeight_doNotCombineTraffic:
    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Total_Weight >= sum{s in S, i in I, u in V, v in V} L[s,i,u,v] * Weight[u,v];

subject to totalWeight_combineBothTraffic:
    combineSourceTraffic == 1 and combineDestTraffic == 1 ==> Total_Weight >= sum{s in S, u in V, v in V} L_s[s,u,v] * Weight[u,v];

## Connection Constraints

subject to totalConnectionsNeeded{g in GroupIndices}:
    Num_Conns_Total >= c_total + FG_Sum[g];

subject to noSelfConnections{s in S}:
    s == d ==> Num_Conn_src[s] = 0;

subject to minNumConnectionsNeededSrc{s in S, g in GroupIndices}:
    Num_Conn_src[s] >= 0 + sum{i in I: s != d} FG_Conn[s,i,g];

subject to maxNumConnectionsNeededSrc{s in S, g in GroupIndices}:
    FG_Conn_s[s,g] == 1 ==> Num_Conn_src[s] <= 1 + sum{i in I: s != d} FG_Conn[s,i,g];

subject to maxNumConnectionsNeededSrcNoFails{s in S}:
    FG_Conn_s_any[s] == 0 ==> Num_Conn_src[s] <= 1;


subject to connSurvivesFromS_1{s in S, g in GroupIndices}:
    connSurvivesFromS[s,g] <= Num_Conn_src[s] - sum{i in I: s != d} FG_Conn[s,i,g];

subject to connSurvivesFromS_2{s in S, g in GroupIndices}:
    connSurvivesFromS[s,g] * card(V)^4 >= Num_Conn_src[s] - sum{i in I: s != d} FG_Conn[s,i,g];

subject to minSrcsReached{g in GroupIndices}:
    sum{s in S} connSurvivesFromS[s,g] >= useMinS;

subject to maxSrcsReached{g in GroupIndices}:
    sum{s in S} connSurvivesFromS[s,g] <= useMaxS;




subject to flowOnlyIfConnectionAndLinkExists{s in S, i in I, u in V, v in V}:
    L[s,i,u,v] <= A[u,v] * C[s,i];

subject to intermediateFlow{s in S, i in I, v in V: v != s and v != d}:
    sum{u in V} L[s,i,u,v] - sum{w in V} L[s,i,v,w] = 0;

subject to sourceFlow{s in S, i in I}:
    sum{u in V} L[s,i,u,s] - sum{w in V} L[s,i,s,w] = -1 * C[s,i];

subject to destinationFlow{s in S, i in I}:
    sum{u in V} L[s,i,u,d] - sum{w in V} L[s,i,d,w] = C[s,i];

subject to flowOnlyInConnection{s in S, i in I, u in V, v in V}:
    L[s,i,u,v] <= C[s,i];

subject to noFlowIntoSource{s in S, i in I, u in V}:
    L[s,i,u,s] = 0;

subject to oneFlowFromNodeInConn{s in S, i in I, u in V}:
    sum{v in V} L[s,i,u,v] <= 1;

subject to oneFlowIntoNodeInConn{s in S, i in I, v in V}:
    sum{u in V} L[s,i,u,v] <= 1;

subject to noReverseFlowIfForward{s in S, i in I, u in V, v in V}:
    L[s,i,u,v] + L[s,i,v,u] <= 1;

# Node is in a connection
subject to nodeInConnection_A{s in S, i in I, v in V}:
    NC[s,i,v] <= sum{u in V} L[s,i,u,v] + sum{w in V} L[s,i,v,w];

subject to nodeInConnection_B{s in S, i in I, v in V}:
    NC[s,i,v] * card(V)^4 >= sum{u in V} L[s,i,u,v] + sum{w in V} L[s,i,v,w];


### L_sd definition constraints

subject to flowOnLinkToDest_A{u in V, v in V}:
    L_d[u,v] <= sum{s in S, i in I} L[s,i,u,v];

subject to flowOnLinkToDest_B{u in V, v in V}:
    L_d[u,v] * card(V)^4 >= sum{s in S, i in I} L[s,i,u,v];

subject to flowOnLinkFromSrc_A{s in S, u in V, v in V}:
    L_s[s,u,v] <= sum{i in I} L[s,i,u,v];

subject to flowOnLinkFromSrc_B{s in S, u in V, v in V}:
    L_s[s,u,v] * card(V)^4 >= sum{i in I} L[s,i,u,v];


## Failure Constraints

# Connection (s,d,i) fails or does not fail due to FG[g]

# Number of failures caused by a link --> Number of connections that include that element. Exclude the src/dest of a connection.
subject to groupCausesConnectionToFail_1{s in S, i in I, g in GroupIndices}:
    FG_Conn[s,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,i,u,v] +
        sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[s,i,v];

subject to groupCausesConnectionToFail_2{s in S, i in I, g in GroupIndices}:
    FG_Conn[s,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,i,u,v] +
        sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[s,i,v];

subject to atLeastOneConnFailsForS_1{s in S, g in GroupIndices}:
    FG_Conn_s[s,g] <= sum{i in I: s != d} FG_Conn[s,i,g];

subject to atLeastOneConnFailsForS_2{s in S, g in GroupIndices}:
    FG_Conn_s[s,g] * card(V) ^ 4 >= sum{i in I: s != d} FG_Conn[s,i,g];

subject to atLeastOneConnFailsForSAny_1{s in S}:
    FG_Conn_s_any[s] <= sum{g in GroupIndices} FG_Conn_s[s,g];

subject to atLeastOneConnFailsForSAny_2{s in S}:
    FG_Conn_s_any[s] * card(V) ^ 4 >= sum{g in GroupIndices} FG_Conn_s[s,g];

# Track connections that fail due to the removal of a failure group - including the src/dest of a connection.
subject to groupCausesConnectionToFailIncludeEndpoints_1{s in S, i in I, g in GroupIndices}:
    FG_Conn_include_endpoints[s,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,i,u,v] +
        sum{v in V: (v,v) in FG[g]} NC[s,i,v];

subject to groupCausesConnectionToFailIncludeEndpoints_2{s in S, i in I, g in GroupIndices}:
    FG_Conn_include_endpoints[s,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,i,u,v] +
        sum{v in V: (v,v) in FG[g]} NC[s,i,v];

# Sum up the number of failed connections due to FG[g]
subject to numFailsDueToGroup{g in GroupIndices}:
    FG_Sum[g] = sum{s in S, i in I} FG_Conn_include_endpoints[s,i,g];

# Put limits on the number of connections between a pair  that can share a FG
subject to connectionsBetweenPairDoNotShareFG{s in S, g in GroupIndices}:
    sum{i in I} FG_Conn[s,i,g] <= 1;

