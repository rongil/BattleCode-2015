package randomQuestions;

import battlecode.common.*;

import java.util.*;

public class randomQuestionsForum {
	public static void main(String[] args){
		double fate;
		
		System.out.println(Math.E);
		
		for(int minerCount = 0; minerCount < 20; minerCount++){
			fate = miningFate(minerCount);
			System.out.println("(" + minerCount + ", " + fate + ")");
		}
	}
	
	public static double miningFate(int minerCount) {
		return Math.pow(Math.E, -minerCount * 0.4);
	}
}