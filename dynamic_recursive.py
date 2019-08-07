import itertools
import random
import json
import glob

import math

import svgwrite as svgwrite
from timeit import default_timer as timer
import drawTemplate as svg


# SET SWAPS HERE
def example():
    global n, L

    n = 15
    L = [[0 for _ in range(n)] for _ in range(n)]
    # left side: clique
    for i in range(4):
        for j in range(i + 1, 5):
            L[i][j] = 2
    # right side: clique
    for i in range(5, n - 1):
        for j in range(i + 1, n):
            L[i][j] = 2
    # mapping of pairs to right side
    k = 5
    for i in range(4):
        for j in range(i + 1, 5):
            L[i][k] = 2
            L[j][k] = 2
            k += 1

    for i in range(n):
        for j in range(i):
            L[i][j] = L[j][i]

    print_matrix()
    print("init")
    init_matrix()
    print("verify")
    verify()
    print("feasibility")
    feasible()
    if len(solutions[len_L]) == 0:
        print(L)
        print_matrix()
        print("NO SOL")
    else:
        print_solution()


# imports a matrix from a JSON file
def importmatrix(filename):
    global n, L
    with open(filename) as fp:
        L = json.load(fp)
        n = len(L)
        for i in range(n):
            for j in range(i + 1, n):
                L[i][j] = abs(L[i][j])


# our matrix generation only generates the top half, but the matrix should be symmetric
def symmetricmatrix():
    global n, L
    for j in range(n):
        for i in range(j + 1, n):
            L[i][j] = L[j][i]


# compute index of all elements after a set of swaps
# also computes a solution with exactly one swap per line
def compute_permutation(swapset):
    # extract all swaps with odd number
    swaps = []
    for i in range(n):
        for j in range(i + 1, n):
            if swapset[i][j] % 2 == 1:
                swaps.append([i, j])

    # compute solution
    sol = {'h': len(swaps),
           's': [],
           'p': [i for i in range(n)]}
    while len(swaps) > 0:
        found = False
        for i in range(len(swaps)):
            swap = swaps[i]
            if abs(sol['p'][swap[0]] - sol['p'][swap[1]]) == 1:
                # swap
                temp = sol['p'][swap[0]]
                sol['p'][swap[0]] = sol['p'][swap[1]]
                sol['p'][swap[1]] = temp
                # add to solution
                sol['s'].append([swap])
                # remove from set
                swaps.pop(i)
                found = True
                break
        if not found:  # no feasible swap
            return 0
    return sol


# can the swaps be simultaneously applied, given index of all elements?
def swaps_feasible(permut, swaps):
    # are they all neighbors?
    for swap in swaps:
        if abs(permut[swap[0]] - permut[swap[1]]) != 1:
            return False
    # is there an element twice?
    occur = [0] * n
    for swap in swaps:
        if occur[swap[0]] > 0:
            return False
        occur[swap[0]] += 1
        if occur[swap[1]] > 0:
            return False
        occur[swap[1]] += 1
    # feasible
    return True


# the index for a matrix is determined by enumeration
def set_to_index(swapset):
    global L
    index = 0
    for i in range(n - 1):
        for j in range(i + 1, n):
            index *= (L[i][j] + 1)
            index += swapset[i][j]
    return index


# a string for the key of a dictionary - not used currently
def set_to_string(swapset):
    string = ""
    for i in range(n - 2):
        for j in range(i + 1, n):
            string += str(swapset[i][j]) + ","
    string += str(swapset[n - 2][n - 1])
    return string


# recreate the matrix from its index
def index_to_set(index):
    global L, n
    len_set = 0
    swap_matrix = [[0 for _ in range(n)] for _ in range(n)]
    for i in range(n - 2, -1, -1):
        for j in range(n - 1, i, -1):
            # print(str(i) + "," + str(j))
            swap_matrix[i][j] = index % (L[i][j] + 1)
            len_set += swap_matrix[i][j]
            index = math.floor(index / (L[i][j] + 1))
    swap_matrix[0][0] = len_set
    return swap_matrix


# extract the unique swaps from a matrix
def unique_swap_list(swap_matrix):
    unique_swaps = []
    for i in range(n):
        for j in range(i + 1, n):
            if swap_matrix[i][j] > 0:
                unique_swaps.append([i, j])
    return unique_swaps


# extract the list of swaps from a matrix
def swap_list(swap_matrix):
    swaps = []
    for i in range(n):
        for j in range(i + 1, n):
            for k in range(swap_matrix[i][j]):
                swaps.append([i, j])
    return swaps


# compute lengths, cummulative matrix and num_sets for enumeration
def init_matrix():
    global n, L, len_L, max_L, num_sets, cum_L
    len_L = max_L = 0
    num_sets = 1
    for i in range(n):
        for j in range(i + 1, n):
            len_L += L[i][j]
            max_L = max(max_L, L[i][j])
            num_sets *= (L[i][j] + 1)
    cum_num_sets = 1
    cum_L = [[0 for _ in range(n)] for _ in range(n)]
    for i in range(n - 2, -1, -1):
        for j in range(n - 1, i, -1):
            cum_L[i][j] = cum_num_sets
            cum_num_sets *= (L[i][j] + 1)


# print matrix
def print_matrix():
    global n, L
    print("swap matrix:")
    for i in range(n):
        for j in range(i + 1):
            print(0, end='')
        for j in range(i + 1, n):
            print(L[i][j], end='')
        print("")


# check feasibility of the instance (find ANY solution)
def feasible():
    global n, L, len_L, solutions, s_time, num_sets
    # every entry is either
    # 0: it has not been computed yet
    # -1: it has no solution
    # or it has a solution and stores the following three attributes
    # 'h': the height of a solution
    # 's': a solution
    # 'p': index of all elements after the swaps
    print(num_sets)
    solutions = [0 for _ in range(num_sets)]

    print("start recursion")
    feasible_rec(num_sets - 1)


# recursively finds a solution for the matrix identified by index
def feasible_rec(index):
    print(index)
    swap_matrix = index_to_set(index)  # find the final permutation
    # hack: store len of set in [0][0]
    set_size = swap_matrix[0][0]
    # we don't store the permutation, but the index of all the elements for easier comparison
    unique_swaps = unique_swap_list(swap_matrix)
    num_unique_swaps = len(unique_swaps)
    simple_sol = compute_permutation(swap_matrix)
    if simple_sol == 0:  # swaps not feasible
        solutions[index] = -1

    # is the simple solution a solution for the set?
    if num_unique_swaps == set_size:
        # start with simple solution
        min_sol = {'h': simple_sol['h'], 's': simple_sol['s'], 'p': simple_sol['p']}
    else:
        # start with 1 more line than swaps
        min_sol = {'h': set_size + 1, 's': [], 'p': simple_sol['p']}

    # feasibility -> need only one swap per line
    for last_swaps in unique_swaps:

        # are the swaps feasible?
        if not swaps_feasible(simple_sol['p'], last_swaps):
            continue

        prev_index = remove_swaps_from_index(index, last_swaps)

        # is prev_set feasible?
        if solutions[prev_index] == 0:
            feasible_rec(prev_index)
        if solutions[prev_index] == -1:
            continue
        prev_sol = solutions[prev_index]

        # new minimum solution
        if prev_sol['h'] + 1 < min_sol['h']:
            min_sol['h'] = prev_sol['h'] + 1
            min_sol['s'] = list(prev_sol['s'])
            min_sol['s'].append(last_swaps)
    solutions[index] = min_sol


# solve the instance (find an optimal solution)
def solve():
    global n, L, len_L, solutions, solution, s_time, num_sets
    # every entry is either
    # 0: it has not been computed yet
    # -1: it has no solution
    # or it has a solution and stores the following three attributes
    # 'h': the height of an optimal solution
    # 's': an optimal solution
    # 'p': index of all elements after the swaps
    solutions = [0 for _ in range(num_sets)]

    solve_rec(num_sets - 1)


# recursively finds an optimal solution for the matrix identified by index
def solve_rec(index):
    swapset = index_to_set(index)  # find the final permutation
    # hack: store len of set in [0][0]
    set_size = swapset[0][0]
    # we don't store the permutation, but the index of all the elements for easier comparison
    simple_swaps = unique_swap_list(swapset)
    num_simple_swaps = len(simple_swaps)
    simple_sol = compute_permutation(swapset)
    if simple_sol == 0:  # swaps not feasible
        solutions[index] = -1

    # is the simple solution a solution for the set?
    if num_simple_swaps == set_size:
        # start with simple solution
        min_sol = {'h': simple_sol['h'], 's': simple_sol['s'], 'p': simple_sol['p']}
    else:
        # no solution yet
        min_sol = -1

    # guess last line
    for num_last_swaps in range(1, max(math.floor(n / 2) + 1, num_simple_swaps)):
        for last_swaps_tupel in itertools.combinations(simple_swaps, num_last_swaps):
            # convert to list
            last_swaps = list(last_swaps_tupel)

            # are the swaps feasible?
            if not swaps_feasible(simple_sol['p'], last_swaps):
                continue

            prev_string = remove_swaps_from_index(index, last_swaps)

            # is prev_set feasible?
            if solutions[prev_string] == 0:
                solve_rec(prev_string)
            prev_sol = solutions[prev_string]
            if prev_sol == -1:
                continue

            # new minimum solution
            if min_sol == -1 or prev_sol['h'] + 1 < min_sol['h']:
                min_sol = {'h': prev_sol['h'] + 1,
                           's': list(prev_sol['s']),
                           'p': simple_sol['p']}
                min_sol['s'].append(last_swaps)

    solutions[index] = min_sol


# quickly calculate the index of the matrix obtained by removing a set of swaps
def remove_swaps_from_index(index, last_swaps):
    # "remove" the last_swaps
    for swap in last_swaps:
        index -= cum_L[swap[0]][swap[1]]
    return index


# print the solution
def print_solution():
    global n, L, len_L, solutions, num_sets
    print("----------")
    if solutions[num_sets - 1] == -1:
        print("no solution")
        return
    print("layers: " + str(solutions[num_sets - 1]['h']))
    print("swaps:")
    # print swaps
    for swaps in solutions[num_sets - 1]['s']:
        for swap in swaps:
            print(str(swap[0]) + "-" + str(swap[1]), end='')
        print("")

    # print permutation
    print("permutations:")
    permut = [i for i in range(n)]
    indexof = [i for i in range(n)]
    for num in permut:
        print(num),
    print("")
    for swaps in solutions[num_sets - 1]['s']:
        for swap in swaps:
            temp = permut[indexof[swap[0]]]
            permut[indexof[swap[0]]] = permut[indexof[swap[1]]]
            permut[indexof[swap[1]]] = temp
            temp = indexof[swap[0]]
            indexof[swap[0]] = indexof[swap[1]]
            indexof[swap[1]] = temp
        for num in permut:
            print(str(num) + " ", end='')
        print("")


# is there a solution?
def verify():
    global L
    if not compute_permutation(L):
        print("instance has no solution")
        exit(0)


# used for the drawing (only relevant for non-random-generated instances)
def get_torsions(matrix):
    return [matrix[i][i] for i in range(len(matrix))]


# drawing a solution - code by Olszewski et al. (adjusted for our data structures)
def draw_svg_template(output, entire_template=False, white=False, scale=1.0):
    colors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22",
              "#17becf", "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5", "#c49c94", "#f7b6d2", "#c7c7c7",
              "#dbdb8d", "#9edae5"]
    global n, len_L, L, solutions
    dwg = svgwrite.Drawing()
    length_torsion = 40 * scale
    length_permut = 100 * scale
    torsions = get_torsions(L)
    max_torsions = abs(max(torsions, key=abs))
    coord = []
    start_height = 100 * scale
    if white:
        colors = ["white"] * 20
    if entire_template:
        start_height += n * 100 * scale
    for i in range(n):
        coord.append([(i * 100 * scale + start_height, start_height) for _ in range(solution['h'] + max_torsions + 1)])
    for i in range(n - 1):
        svg.upperSemiCircle(coord[i][0][0] + 40 * scale, coord[i][0][1], coord[i + 1][0][0], coord[i + 1][0][1], dwg,
                            scale)
    for i in range(1, max_torsions + 1):
        for j in range(len(coord)):
            coord[j][i] = (coord[j][i - 1][0], coord[j][i - 1][1] + length_torsion)
            # make torsion
            if torsions[j] == 0:
                svg.straightTransition(coord[j][i - 1][0], coord[j][i - 1][1], dwg, length_torsion, colors[j % 20],
                                       scale)
            elif torsions[j] > 0:
                svg.positiveTorsion(coord[j][i - 1][0], coord[j][i - 1][1], dwg, colors[j % 20], scale)
                torsions[j] -= 1
            else:
                svg.negativeTorsion(coord[j][i - 1][0], coord[j][i - 1][1], dwg, colors[j % 20], scale)
                torsions[j] += 1
    for i in range(max_torsions + 1, len(coord[0])):
        the_round = solution['s'][i - max_torsions - 1]
        for transformation in the_round:
            coord[transformation[0]][i], coord[transformation[1]][i] = \
                (coord[transformation[1]][i - 1][0], coord[transformation[1]][i - 1][1] + length_permut), \
                (coord[transformation[0]][i - 1][0], coord[transformation[0]][i - 1][1] + length_permut)
            # make permut
            # positive permutation
            if L[transformation[0]][transformation[1]] > 0:
                if coord[transformation[0]][i - 1][0] < coord[transformation[1]][i - 1][0]:
                    svg.leftPermut(coord[transformation[0]][i - 1][0], coord[transformation[0]][i - 1][1],
                                   dwg, colors[transformation[0] % 20], scale)
                    svg.rightPermut(coord[transformation[1]][i - 1][0], coord[transformation[1]][i - 1][1],
                                    dwg, colors[transformation[1] % 20], scale)
                else:
                    svg.leftPermut(coord[transformation[1]][i - 1][0], coord[transformation[1]][i - 1][1],
                                   dwg, colors[transformation[1] % 20], scale)
                    svg.rightPermut(coord[transformation[0]][i - 1][0], coord[transformation[0]][i - 1][1],
                                    dwg, colors[transformation[0] % 20], scale)
            # negative permutation
            else:
                if coord[transformation[0]][i - 1][0] < coord[transformation[1]][i - 1][0]:
                    svg.rightPermut(coord[transformation[1]][i - 1][0], coord[transformation[1]][i - 1][1],
                                    dwg, colors[transformation[1] % 20], scale)
                    svg.leftPermut(coord[transformation[0]][i - 1][0], coord[transformation[0]][i - 1][1],
                                   dwg, colors[transformation[0] % 20], scale)
                else:
                    svg.rightPermut(coord[transformation[0]][i - 1][0], coord[transformation[0]][i - 1][1],
                                    dwg, colors[transformation[0] % 20], scale)
                    svg.leftPermut(coord[transformation[1]][i - 1][0], coord[transformation[1]][i - 1][1],
                                   dwg, colors[transformation[1] % 20], scale)
        for j in range(len(coord)):
            if coord[j][i] == coord[j][0]:
                coord[j][i] = (coord[j][i - 1][0], coord[j][i - 1][1] + length_permut)
                # draw straight line
                svg.straightTransition(coord[j][i - 1][0], coord[j][i - 1][1], dwg, length_permut, colors[j % 20], scale)
    final_index_of = compute_permutation(L)['p']
    final_position = [0] * n
    for i in range(n):
        final_position[final_index_of[i]] = i
    left_coord = ([coord[final_position[0]][-1][0], coord[final_position[0]][-1][1] + 100 * scale])
    right_coord = ([coord[final_position[-1]][-1][0] + 40 * scale, coord[final_position[-1]][-1][1] + 100 * scale])
    for position in final_position:
        svg.bottom(coord[position][-1][0], coord[position][-1][1], left_coord[0], left_coord[1], right_coord[0],
                   right_coord[1],
                   dwg, colors[position % 20], scale)
    x, y = coord[0][0][0], coord[0][0][1]
    x2, y2 = coord[-1][0][0] + 40 * scale, coord[-1][0][1]
    if not entire_template:
        top_left = (x, y)
        top_right = (x2, y2)
        svg.top(top_left[0], top_left[1], top_right[0], top_right[1], dwg, scale)
    else:
        x3, y3 = x - 80 * scale, y
        x4, y4 = x3 - (x2 - x), y
        x5, y5 = coord[final_position[0]][-1][0], coord[final_position[0]][-1][1] + 100 * scale
        x6, y6 = coord[final_position[-1]][-1][0] + 40 * scale, y5
        x7, y7 = x3, y5
        x8, y8 = x4, y5
        svg.upperSemiCircle(x3 + 1, y3, x, y, dwg, scale)
        svg.upperSemiCircle(x4 + 1, y4, x2 - 1, y2, dwg, scale)
        svg.lowerSemiCircle(x7 + 1, y7, x5, y5, dwg, scale)
        svg.lowerSemiCircle(x8 + 1, y8, x6 - 1, y6, dwg, scale)
        svg.straightLine(x3, y3, y5 - y, dwg, scale)
        svg.straightLine(x4, y4, y5 - y, dwg, scale)
    dwg.write(output)


# draw a solution and save it in the specified file
def draw_solution(filename):
    global len_L, solutions, solution
    solution = solutions[num_sets - 1]
    with open(filename + ".svg", "w+") as output:
        draw_svg_template(output)


# reads all files in the folder examples with the given prefix,
# solves the instances,
# draws the solutions, and
# writes the time spent to a csv file
def experiments(pre, rep):
    global len_L, s_time, solutions, num_sets

    for filename in sorted(glob.glob("examples/" + pre + "*.json")):
        print(filename[9:-5] + ": ")
        time_list = [0] * rep
        importmatrix(filename)
        init_matrix()
        verify()
        with open("output/" + pre + "_rec.csv", 'a') as outfile:
            outfile.write(filename[9:-5] + "," + str(len_L) + ",Ours")
            for i in range(rep):
                # print_matrix()
                start_this = timer()
                s_time = start_this
                solve()
                end_this = timer()
                # print_solution()
                if not solutions[num_sets - 1] == -1:
                    draw_solution("our_drawings/" + filename[9:-5])
                time_list[i] = end_this - start_this
                outfile.write("," + str(time_list[i]))
            avg_time = sum(time_list) / rep
            print(avg_time)
            outfile.write("," + str(avg_time) + "\n")


# generates a random matrix by random sampling with
# at most maxnum swaps per pair
# at least min_len swaps in total
# at most max_len swaps in total
def generate_instance(maxnum, min_len, max_len):
    global n, L, len_L
    valid = False
    while not valid:
        L = [[0 for _ in range(n)] for _ in range(n)]
        len_L = 0
        for i in range(n - 1):
            for j in range(i + 1, n):
                L[i][j] = random.randint(0, maxnum)
                len_L += L[i][j]
        if min_len <= len_L <= max_len:
            valid = compute_permutation(L)


# randomly generates a number of instances and stores them in folder "examples"
def generate_instances(number, max_num, min_len, max_len):
    global len_L
    for i in range(number):
        generate_instance(max_num, min_len, max_len)
        symmetricmatrix()
        init_matrix()
        filename = "extra_" + str(n) + "x" + str(n) + "_" + str(len_L) + "-" + str(i)
        print(filename)
        with open("examples/" + filename + ".json", 'w') as outfile:
            json.dump(L, outfile)
        print(L)


# initialize the csv file for the experiments
def init_output(pre, rep):
    with open("output/" + pre + "_rec.csv", 'w+') as outfile:
        outfile.write("Instance,Swaps,Group")
        for i in range(1, rep + 1):
            outfile.write(",Run_" + str(i))
        outfile.write(",Avg\n")
        outfile.close()


n = len_L = max_L = num_sets = s_time = 0
L, solutions, solution, cum_L = [], [], [], []

# n = 7
# for i in range(22,50):
#    print(i)
#    generate_instances(10,int(5*i/(n*n) + 1),i,i)

repetitions = 5
# prefix = "5x5"
# prefix = "6x6"
# prefix = "7x7"
prefix = "extra_5x5"
# prefix = "extra_6x6"
# prefix = "extra_7x7"
init_output(prefix, repetitions)
experiments(prefix, repetitions)
