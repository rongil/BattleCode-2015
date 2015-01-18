package randomQuestions;

import battlecode.common.*;

public class randomQuestionsForum {
	public static void main(String[] args){
		int number = 10;
		
		String string = Integer.toString(number);
		string += "000";
		
		number = Integer.parseInt(string);
		System.out.println(number);
	}
}