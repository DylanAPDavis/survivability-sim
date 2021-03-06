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
param Weight{u in V, v in V} default 0;

# S - Sources
set S;

# D - Destinations
set D;

# F - Failure set
set F within AllPairs default {};

# SD - Source/Dest pairs
set SD within {S cross D} default {s in S, d in D: s != d};

# c_total - Total number of connections needed after k failures
param c_total >= 0 integer default card(SD);

# c_min - Minimum number of connections per (s,d) pair that need to survive k failures
param c_min_sd{SD} >= 0 integer default 0;

# c_max - Maxmimum number of connections per (s,d) pair that need to survive k failures
param c_max_sd{SD} >= 0 integer default c_total;


# ENDPOINT PARAMETERS
# c_min - Minimum number of connections from s in S that need to survive
param c_min_s{s in S} >= 0 integer default 0;

# c_max - Maxmimum number of connections from s in S  that need to survive
param c_max_s{s in S} >= 0 integer default c_total;

# c_min - Minimum number of connections to d in D that need to survive
param c_min_d{d in D} >= 0 integer default 0;

# c_max - Maxmimum number of connections to d in D that need to survive
param c_max_d{d in D} >= 0 integer default c_total;
# END ENDPOINT PARAMETERS

# NumGroups - Number of k-sized failure groups
param NumGroups default 1;

# GroupIndices - Indexing set for all groups of failure elements of size k
set GroupIndices := 1..NumGroups;

# FG - Set of all failure groups of size k
set FG {g in GroupIndices} within AllPairs default {};

# Traffic combination
param combineSourceTraffic binary default 0;
param combineDestTraffic binary default 0;

# MIN AND MAX PARAMS FOR NUMBER OF SRCS/DESTINATIONS
param useMinS >= 0 integer default 0;
param useMaxS >= 0 integer default card(S);
param useMinD >= 0 integer default 0;
param useMaxD >= 0 integer default card(D);

# The number of failure events that will happen
param nfe default 0;

set SinF = {s in S : (s,s) in F};
set SnotF = {s in S : s not in SinF};

param minSrcFailures = min(card(SinF), nfe);
param maxSrcNotFailed = useMinS;
param sRequired = minSrcFailures + maxSrcNotFailed;

set DinF = {d in D : (d,d) in F};#D inter F;
set DnotF = {d in D : d not in DinF};#D diff F;

param minDestFailures = min(card(DinF), nfe);
param maxDestsNotFailed = useMinD;#min(useMinD, card(DnotF));
param dRequired = minDestFailures + maxDestsNotFailed;


# VARIABLES

# C - connection number (i) from node s to node d
var C{(s,d) in SD, i in I} binary;

# L - Link flow on (u, v) used by Connection (s, d, i)
var L{(s,d) in SD, i in I, u in V, v in V} binary;

# There is at least one link flow coming from source s
#var L_s{s in S, u in V, v in V} binary;

# There is at least one link flow going to destination d
#var L_d{d in D, u in V, v in V} binary;

# There is at least one link flow between pair (s,d)
#var L_sd{(s,d) in SD, u in V, v in V} binary;

# NC - Node v is in Connection (s,d,i)
var NC{(s,d) in SD, i in I, v in V} binary;

# Connection (s,d,i) fails because of the removal of FG[g]. Excludes the source and destination
var FG_Conn {(s,d) in SD, i in I, g in GroupIndices} binary;

# Connection (s,d,i) can be disconnected by the removal of any members of FG[g] - including sources and destinations
var FG_Conn_include_endpoints {(s,d) in SD, i in I, g in GroupIndices} binary;


# INDICATOR VARIABLES -> Indicate if at least one connection is destroyed by removal of FG g

# FLOW
# At least one connection from (s,d) is disconnected by removal of FG[g]
var FG_Conn_sd{(s,d) in SD, g in GroupIndices} binary;

# At least one connection from (s,d) is disconnected by removal of any FG
var FG_Conn_sd_any{(s,d) in SD} binary;

# ENDPOINT

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



#### Survival variables - determine if sufficient srcs/dsts are connected
# For sources
var connSurvivesFromS{s in S, g in GroupIndices} binary;
var srcConnected{s in S} binary;
var numSrcsDisconnected{g in GroupIndices} >= 0 integer;
var maxSrcsDisconnected >= 0 integer;
# For destinations
var connSurvivesToD{d in D, g in GroupIndices} binary;
var destConnected{d in D} binary;
var numDestsDisconnected{g in GroupIndices} >= 0 integer;
var maxDestsDisconnected >= 0 integer;
####




# FG_Sum - Number of failed connections caused by this failure group (groupIndex)
var FG_Sum {g in GroupIndices} >= 0 integer;

# Number of connections
var Num_Conn{(s,d) in SD} = sum{i in I} C[s,d,i];

# ENDPOINT VARIABLES
# Number of connections from a source s
var Num_Conn_src{s in S} = sum{d in D, i in I: s != d} C[s,d,i];

# Number of connections to a destination d
var Num_Conn_dst{d in D} = sum{s in S, i in I: s != d} C[s,d,i];
# END ENDPOINT VARIABLES


# OBJECTIVE VARIABLES

# Number of connections total
var Num_Conns_Total = sum{(s,d) in SD} Num_Conn[s,d];

# Number of link usages
var Num_Links_Used >= 0 integer;

# Total weight of all used links
var Total_Weight >= 0;


# OBJECTIVE

#minimize linksused:
#	Num_Links_Used;

#minimize connections:
#	Num_Conns_Total;

minimize totalcost:
	Total_Weight;

subject to totalWeightDefinition:
    Total_Weight = sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v] * Weight[u,v];

# Objective definition constraints
# LINKS USED
#subject to linksUsed_combineTraffic_SourceOnly:
#    combineSourceTraffic == 1 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{s in S, u in V, v in V} L_s[s,u,v];

#subject to linksUsed_combineTraffic_DestOnly:
#    combineSourceTraffic == 0 and combineDestTraffic == 1 ==> Num_Links_Used >= sum{d in D, u in V, v in V} L_d[d,u,v];

#subject to linksUsed_doNotCombineTraffic:
#    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Num_Links_Used >= sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v];

#subject to linksUsed_combineBothTraffic:
#    combineSourceTraffic == 1 and combineDestTraffic == 1 ==> Num_Links_Used >= sum{(s,d) in SD, u in V, v in V} L_sd[s,d,u,v];

# TOTAL WEIGHT
#subject to totalWeight_combineTraffic_SourceOnly:
#    combineSourceTraffic == 1 and combineDestTraffic == 0 ==> Total_Weight >= sum{s in S, u in V, v in V} L_s[s,u,v] * Weight[u,v];

#subject to totalWeight_combineTraffic_DestOnly:
#    combineSourceTraffic == 0 and combineDestTraffic == 1 ==> Total_Weight >= sum{d in D, u in V, v in V} L_d[d,u,v] * Weight[u,v];

#subject to totalWeight_doNotCombineTraffic:
#    combineSourceTraffic == 0 and combineDestTraffic == 0 ==> Total_Weight >= sum{(s,d) in SD, i in I, u in V, v in V} L[s,d,i,u,v] * Weight[u,v];

#subject to totalWeight_combineBothTraffic:
#    combineSourceTraffic == 1 and combineDestTraffic == 1 ==> Total_Weight >= sum{(s,d) in SD, u in V, v in V} L_sd[s,d,u,v] * Weight[u,v];

## Connection Constraints

subject to totalConnectionsNeeded{g in GroupIndices}:
	Num_Conns_Total >= c_total + FG_Sum[g];

# Flow Constraints
subject to minNumConnectionsNeeded{(s,d) in SD, g in GroupIndices}:
	Num_Conn[s,d] >= c_min_sd[s,d] + sum{i in I} FG_Conn[s,d,i,g];

subject to maxNumConnectionsNeededFails{(s,d) in SD, g in GroupIndices}:
	FG_Conn_sd[s,d,g] == 1 ==> Num_Conn[s,d] <= c_max_sd[s,d] + sum{i in I} FG_Conn[s,d,i,g];

subject to maxNumConnectionsNeededNoFails{(s,d) in SD}:
	FG_Conn_sd_any[s,d] == 0 ==> Num_Conn[s,d] <= c_max_sd[s,d];

# ENDPOINT CONSTRAINTS

# Source constraints
subject to minNumConnectionsNeededSource{s in S, g in GroupIndices}:
	Num_Conn_src[s] >= c_min_s[s] + sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

#subject to maxNumConnectionsNeededSource{s in S, g in GroupIndices}:
#	FG_Conn_s[s,g] == 1 ==> Num_Conn_src[s] <= c_max_s[s] + sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

#subject to maxNumConnectionsNeededSourceNoFails{s in S}:
#	FG_Conn_s_any[s] == 0 ==> Num_Conn_src[s] <= c_max_s[s];

# Destination Constraints

subject to minNumConnectionsNeededDest{d in D, g in GroupIndices}:
	Num_Conn_dst[d] >= c_min_d[d] + sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

#subject to maxNumConnectionsNeededDest{d in D, g in GroupIndices}:
#	FG_Conn_d[d,g] == 1 ==> Num_Conn_dst[d] <= c_max_d[d] + sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

#subject to maxNumConnectionsNeededDestNoFails{d in D}:
#	FG_Conn_d_any[d] == 0 ==> Num_Conn_dst[d] <= c_max_d[d];

#END ENDPOINT CONSTRAINTS

##### SRC/DEST REACHABILITY CONSTRAINTS

### SOURCES
subject to connSurvivesFromS_1_LessThanS{s in S, g in GroupIndices}:#: sRequired < card(S)}:
   connSurvivesFromS[s,g] <= Num_Conn_src[s] - sum{d in D, i in I: s != d} FG_Conn_include_endpoints[s,d,i,g];

subject to connSurvivesFromS_2_LessThanS{s in S, g in GroupIndices}:#: sRequired < card(S)}:
   connSurvivesFromS[s,g] * card(V)^4 >= Num_Conn_src[s] - sum{d in D, i in I: s != d} FG_Conn_include_endpoints[s,d,i,g];

subject to connSurvivesFromS_1_GreaterThanS{s in S, g in GroupIndices: sRequired >= card(S)}:
	connSurvivesFromS[s,g] <= Num_Conn_src[s] - sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];

subject to connSurvivesFromS_2_GreaterThanS{s in S, g in GroupIndices: sRequired >= card(S)}:
	connSurvivesFromS[s,g] * card(V)^4 >= Num_Conn_src[s] - sum{d in D, i in I: s != d} FG_Conn[s,d,i,g];


subject to srcConnected_1{s in S}:
    srcConnected[s] <= Num_Conn_src[s];

subject to srcConnected_2{s in S}:
    srcConnected[s] * card(V)^4 >= Num_Conn_src[s];

subject to numSrcsThatAreDisconnected{g in GroupIndices}:
    numSrcsDisconnected[g] = sum{s in S} srcConnected[s] - sum{s in S} connSurvivesFromS[s,g];

subject to greatestedNumSrcDisconnected{g in GroupIndices}:
    maxSrcsDisconnected >= numSrcsDisconnected[g];

subject to minSourcesThatMustBeConnected_LessThanS{if sRequired < card(S)}:
	sum{s in S} srcConnected[s] >= useMinS + maxSrcsDisconnected;

subject to maxSourcesThatMustBeConnected_LessThanS{if sRequired < card(S)}:
	sum{s in S} srcConnected[s] <= useMaxS + maxSrcsDisconnected;

subject to minSourcesThatMustBeConnected_GreaterThanS{g in GroupIndices: sRequired >= card(S)}:
	sum{s in S} connSurvivesFromS[s,g] >= card(S);
### END SOURCES

### START DESTINATIONS
subject to connSurvivesToD_1_LessThanD{d in D, g in GroupIndices: dRequired < card(D)}:
   connSurvivesToD[d,g] <= Num_Conn_dst[d] - sum{s in S, i in I: s != d} FG_Conn_include_endpoints[s,d,i,g];

subject to connSurvivesToD_2_LessThanD{d in D, g in GroupIndices: dRequired < card(D)}:
   connSurvivesToD[d,g] * card(V)^4 >= Num_Conn_dst[d] - sum{s in S, i in I: s != d} FG_Conn_include_endpoints[s,d,i,g];

subject to connSurvivesToD_1_GreaterThanD{d in D, g in GroupIndices: dRequired >= card(D)}:
	connSurvivesToD[d,g] <= Num_Conn_dst[d] - sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

subject to connSurvivesToD_2_GreaterThanD{d in D, g in GroupIndices: dRequired >= card(D)}:
	connSurvivesToD[d,g] * card(V)^4 >= Num_Conn_dst[d] - sum{s in S, i in I: s != d} FG_Conn[s,d,i,g];

subject to destConnected_1{d in D}:
	destConnected[d] <= Num_Conn_dst[d];

subject to destConnected_2{d in D}:
	destConnected[d] * card(V)^4 >= Num_Conn_dst[d];

subject to numDestsThatAreDisconnected{g in GroupIndices}:
	numDestsDisconnected[g] = sum{d in D} destConnected[d] - sum{d in D} connSurvivesToD[d,g];

subject to greatestedNumDestsDisconnected{g in GroupIndices}:
	maxDestsDisconnected >= numDestsDisconnected[g];

subject to minDestsThatMustBeConnected_LessThanD{if dRequired < card(D)}:
	sum{d in D} destConnected[d] >= useMinD + maxDestsDisconnected;

subject to maxDestsThatMustBeConnected_LessThanD{if dRequired < card(D)}:
	sum{d in D} destConnected[d] <= useMaxD + maxDestsDisconnected;

subject to minDestsThatMustBeConnected_NeedAll{g in GroupIndices: useMinD >= card(D)}:
	sum{d in D} connSurvivesToD[d,g] = card(D);

subject to minDestsThatMustBeConnected_GreaterThanD{if dRequired >= card(D)}:
	sum{d in D} destConnected[d] = card(D);

### END DESTINATIONS

##### END SRC/DEST REACHABILITY CONSTRAINTS



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



### L_s, L_d, L_sd definition constraints
#subject to flowOnLinkFromSource_A{s in S, u in V, v in V}:
#	L_s[s,u,v] <= sum{d in D, i in I: s != d} L[s,d,i,u,v];

#subject to flowOnLinkFromSource_B{s in S, u in V, v in V}:
#	L_s[s,u,v] * card(V)^4 >= sum{d in D, i in I: s != d} L[s,d,i,u,v];

#subject to flowOnLinkToDestination_A{d in D, u in V, v in V}:
#	L_d[d,u,v] <= sum{s in S, i in I: s != d} L[s,d,i,u,v];

#subject to flowOnLinkToDestination_B{d in D, u in V, v in V}:
#	L_d[d,u,v] * card(V)^4 >= sum{s in S, i in I: s != d} L[s,d,i,u,v];

#subject to flowOnLinkBetweenPair_A{(s,d) in SD, u in V, v in V}:
#	L_sd[s,d,u,v] <= sum{i in I} L[s,d,i,u,v];

#subject to flowOnLinkBetweenPair_B{(s,d) in SD, u in V, v in V}:
#	L_sd[s,d,u,v] * card(V)^4 >= sum{i in I} L[s,d,i,u,v];



## Failure Constraints

# Connection (s,d,i) fails or does not fail due to FG[g]

# Number of failures caused by a link --> Number of connections that include that element. Exclude the src/dest of a connection.
subject to groupCausesConnectionToFail_1{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[s,d,i,v];

subject to groupCausesConnectionToFail_2{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: v != s and v != d and (v,v) in FG[g]} NC[s,d,i,v];

# Track connections that fail due to the removal of a failure group - including the src/dest of a connection.
subject to groupCausesConnectionToFailIncludeEndpoints_1{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn_include_endpoints[s,d,i,g] <= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[g] and v != s} NC[s,d,i,v];

subject to groupCausesConnectionToFailIncludeEndpoints_2{(s,d) in SD, i in I, g in GroupIndices}:
	FG_Conn_include_endpoints[s,d,i,g] * card(V)^4 >= sum{u in V, v in V: u != v and ((u,v) in FG[g] or (v,u) in FG[g])} L[s,d,i,u,v] + sum{v in V: (v,v) in FG[g] and v != s} NC[s,d,i,v];

# Sum up the number of failed connections due to FG[g]
subject to numFailsDueToGroup{g in GroupIndices}:
	FG_Sum[g] = sum{(s,d) in SD, i in I} FG_Conn[s,d,i,g];


# INDICATOR VARIABLE CONSTRAINTS

# Flow pairs
subject to atLeastOneConnFailsForSD_1{(s,d) in SD, g in GroupIndices}:
    FG_Conn_sd[s,d,g] <= sum{i in I} FG_Conn[s,d,i,g];

subject to atLeastOneConnFailsForSD_2{(s,d) in SD, g in GroupIndices}:
    FG_Conn_sd[s,d,g] * card(V) ^ 4 >= sum{i in I} FG_Conn[s,d,i,g];

# Any group G fails
subject to atLeastOneConnFailsForSDAny_1{(s,d) in SD}:
    FG_Conn_sd_any[s,d] <= sum{g in GroupIndices} FG_Conn_sd[s,d,g];

subject to atLeastOneConnFailsForSDAny_2{(s,d) in SD}:
    FG_Conn_sd_any[s,d] * card(V) ^ 4 >= sum{g in GroupIndices} FG_Conn_sd[s,d,g];


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


# A failure element should only appear at most once in a pair's connections
#subject to fAtMostOncePair{s in S, d in D, u in V, v in V, g in GroupIndices: u != v and ((u,v) in FG[g] or (v,u) in FG[g])}:
#subject to fAtMostOncePair{s in S, u in V, v in V, g in GroupIndices: u != v and ((u,v) in FG[g] or (v,u) in FG[g])}:
#	sum{i in I} L[s,d,i,u,v] <= 1;

#subject to fAtMostOncePair_nodes{s in S, d in D, v in V, g in GroupIndices: (v,v) in FG[g] and v != s and v != d}:
#subject to fAtMostOncePair_nodes{s in S, v in V, g in GroupIndices: (v,v) in FG[g] and v != s and v != d}:
#	sum{i in I} NC[s,d,i,v] <= 1;

#-------------------------------------------------------
