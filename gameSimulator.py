#taken from schnitzel repository
#link: https://github.com/david-westreicher/schnitzel/blob/master/util/gameall.py#L7

from tempfile import mkstemp
from shutil import move
from subprocess import Popen, PIPE
from os import remove, close, listdir
import re
import time
import sys


def replace(file_path, pattern, subst):
    #Create temp file
	fh, abs_path = mkstemp()
	new_file = open(abs_path,'w')
	old_file = open(file_path)
	for line in old_file:
		new_file.write(re.sub(pattern,subst, line))
    #close temp file
	new_file.close()
	close(fh)
	old_file.close()
    #Remove original file
	remove(file_path)
    #Move new file
	move(abs_path, file_path)


def analyseOutput(output):
	for line in output.split('\n'):
		if "[server]" in line and "wins" in line:
			print line
			if ourteam in line:
				return 1
			else:
				return 0

def printStats():
	saveFile.write("enemy,")
	for map in listdir(mapFolder):
		saveFile.write(map+",")
	saveFile.write("sum")
	saveFile.write("\n")
		

#set team A to our team
saveFile = open('test.csv', 'w')
ourteam = "team209"
mapFolder = "maps"
enemiesFolder = "teams"
stats = dict()
replace("bc.conf", "bc.game.team-a=.*","bc.game.team-a="+ourteam)
enemies = listdir(enemiesFolder)
if len(sys.argv)==2:
	enemies = [str(sys.argv[1])]
maps = listdir(mapFolder)
matches = len(enemies)*len(maps)
currentMatch = 0
printStats()
for enemy in enemies:
	replace("bc.conf", "bc.game.team-b=.*","bc.game.team-b="+enemy)
	saveFile.write(enemy +",")
	stats[enemy] = dict()
	sum = 0
	for map in maps:
		replace("bc.conf", "bc.game.maps=.*","bc.game.maps="+map)
		currentMatch+=1
		print "team209 vs "+enemy+" on "+map+" ["+str(currentMatch)+"/"+str(matches)+"]"
		time.sleep(1)
		output = Popen(["ant","file"], stdout=PIPE).communicate()[0]
		win = analyseOutput(output);
		stats[enemy][map] = win
		if win==1:
			sum+=1
		saveFile.write(str(stats[enemy][map])+",")
		saveFile.flush()
	saveFile.write(str(sum/float(len(maps))*100))
	saveFile.write("\n")
