import sys
import numpy
import ast
import glob

import svgwrite as svgwrite
import drawTemplate as svg


# compute index of all elements after a set of swaps
# also computes a solution with exactly one swap per line
def compute_permutation(swapset):
    # extract all swaps with odd number
    swaps = []
    for i in range(n):
        for j in range(i + 1, n):
            if swapset[i][j] % 2 == 1:
                swaps.append([i, j])

    #compute solution
    sol = {'h': len(swaps),
           's': [],
           'p': range(n)}
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
    # permut = range(n)
    # for i in range(n): # write permutation
    #    permut[sol['p'][i]] = i
    return sol

def getTorsions(matrix):
    return [matrix[i][i] for i in range(len(matrix))]

def drawSVGTemplateFullInput(output, n_param, len_L_param, L_param, solution_param, entireTemplate=False, white=False, scale=1.0):
    global n, len_L, L, solution
    n = n_param
    len_L = len_L_param
    L = L_param
    solution = solution_param
    colors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22",
              "#17becf", "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5", "#c49c94", "#f7b6d2", "#c7c7c7",
              "#dbdb8d", "#9edae5"]
    dwg = svgwrite.Drawing()
    lengthTorsion = 40 * scale
    lengthPermut = 100 * scale
    torsions = getTorsions(L)
    maxTorsions = abs(max(torsions, key=abs))
    coord = []
    start_height = 100 * scale
    if white:
        colors = ["white"] * 20
    if entireTemplate:
        start_height += n * 100 * scale
    for i in range(n):
        coord.append([(i * 100 * scale + start_height, start_height) for j in range(solution['h']+maxTorsions+1)])
    for i in range(n-1):
        svg.upperSemiCircle(coord[i][0][0] + 40 * scale, coord[i][0][1], coord[i+1][0][0], coord[i+1][0][1], dwg, scale)
    for i in range(1,maxTorsions+1):
        for j in range(len(coord)):
            coord[j][i] = (coord[j][i-1][0], coord[j][i-1][1]+lengthTorsion)
            #make torsion
            if torsions[j] == 0:
                svg.straightTransition(coord[j][i-1][0], coord[j][i-1][1], dwg, lengthTorsion, colors[j%20], scale)
            elif torsions[j] > 0 :
                svg.positiveTorsion(coord[j][i-1][0], coord[j][i-1][1], dwg, colors[j%20], scale)
                torsions[j] -= 1
            else:
                svg.negativeTorsion(coord[j][i-1][0], coord[j][i-1][1], dwg, colors[j%20], scale)
                torsions[j] += 1
    for i in range(maxTorsions+1, len(coord[0])):
        round = solution['s'][i - maxTorsions - 1]
        for transformation in round:
            coord[transformation[0]][i], coord[transformation[1]][i] = \
                (coord[transformation[1]][i-1][0],coord[transformation[1]][i-1][1]+lengthPermut), \
                (coord[transformation[0]][i-1][0],coord[transformation[0]][i-1][1]+lengthPermut)
            #make permut
            #positive permutation
            if L[transformation[0]][transformation[1]] > 0:
                if coord[transformation[0]][i-1][0] < coord[transformation[1]][i-1][0]:
                    svg.leftPermut(coord[transformation[0]][i-1][0], coord[transformation[0]][i-1][1],
                                   dwg, colors[transformation[0]%20], scale)
                    svg.rightPermut(coord[transformation[1]][i-1][0], coord[transformation[1]][i-1][1],
                                    dwg, colors[transformation[1]%20], scale)
                else :
                    svg.leftPermut(coord[transformation[1]][i-1][0], coord[transformation[1]][i-1][1],
                                   dwg, colors[transformation[1]%20], scale)
                    svg.rightPermut(coord[transformation[0]][i-1][0], coord[transformation[0]][i-1][1],
                                    dwg, colors[transformation[0]%20], scale)
            #negative permutation
            else :
                if coord[transformation[0]][i-1][0] < coord[transformation[1]][i-1][0]:
                    svg.rightPermut(coord[transformation[1]][i-1][0], coord[transformation[1]][i-1][1],
                                    dwg, colors[transformation[1]%20], scale)
                    svg.leftPermut(coord[transformation[0]][i-1][0], coord[transformation[0]][i-1][1],
                                   dwg,colors[transformation[0]%20], scale)
                else :
                    svg.rightPermut(coord[transformation[0]][i-1][0], coord[transformation[0]][i-1][1],
                                    dwg,colors[transformation[0]%20], scale)
                    svg.leftPermut(coord[transformation[1]][i-1][0], coord[transformation[1]][i-1][1],
                                   dwg,colors[transformation[1]%20], scale)
        for j in range(len(coord)):
            if coord[j][i] == coord[j][0]:
                coord[j][i] = (coord[j][i-1][0], coord[j][i-1][1]+lengthPermut)
                #draw straight line
                svg.straightTransition(coord[j][i-1][0], coord[j][i-1][1], dwg, lengthPermut, colors[j%20], scale)
    finalIndexOf = compute_permutation(L)['p']
    finalPosition = [0] * n
    for i in range(n):
        finalPosition[finalIndexOf[i]] = i
    left_coord = ([coord[finalPosition[0]][-1][0], coord[finalPosition[0]][-1][1] + 100 * scale])
    right_coord = ([coord[finalPosition[-1]][-1][0] + 40 * scale, coord[finalPosition[-1]][-1][1] + 100 * scale])
    for position in finalPosition:
        svg.bottom(coord[position][-1][0], coord[position][-1][1], left_coord[0], left_coord[1], right_coord[0], right_coord[1],
                   dwg, colors[position%20], scale)
    x,y = coord[0][0][0], coord[0][0][1]
    x2,y2 = coord[-1][0][0] + 40 * scale, coord[-1][0][1]
    if not entireTemplate:
        top_left = (x, y)
        top_right = (x2, y2)
        svg.top(top_left[0], top_left[1], top_right[0], top_right[1], dwg, scale)
    else:
        x3,y3 = x - 80 * scale, y
        x4,y4 = x3 - (x2 - x), y
        x5,y5 = coord[finalPosition[0]][-1][0], coord[finalPosition[0]][-1][1] + 100 * scale
        x6,y6 = coord[finalPosition[-1]][-1][0] + 40 * scale, y5
        x7,y7 = x3, y5
        x8,y8 = x4, y5
        svg.upperSemiCircle(x3 + 1, y3, x, y, dwg, scale)
        svg.upperSemiCircle(x4 + 1, y4, x2 - 1, y2, dwg, scale)
        svg.lowerSemiCircle(x7 + 1, y7, x5, y5, dwg, scale)
        svg.lowerSemiCircle(x8 + 1, y8, x6 - 1, y6, dwg, scale)
        svg.straightLine(x3, y3, y5 - y, dwg, scale)
        svg.straightLine(x4, y4, y5 - y, dwg, scale)
    dwg.write(output)


drawSVGTemplateFullInput(open(sys.argv[1],"w+"), numpy.int_(sys.argv[2]), numpy.int_(sys.argv[3]), ast.literal_eval(sys.argv[4]), ast.literal_eval(sys.argv[5]))
