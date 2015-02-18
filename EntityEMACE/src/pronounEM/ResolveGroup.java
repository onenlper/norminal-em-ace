package pronounEM;

import java.io.Serializable;
import java.util.ArrayList;

import model.EntityMention;
import model.EntityMention.Animacy;
import model.EntityMention.Gender;
import model.EntityMention.Number;
import model.EntityMention.Person;

public class ResolveGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

//	String pronoun;
	
	short pronoun;

	ArrayList<Entry> entries;
	
	public ResolveGroup(short pro) {
		this.pronoun = pro;
		this.entries = new ArrayList<Entry>();
	}

	public static class Entry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Context context;
		String head;
		
		Animacy animacy;
		Gender gender;
		Person person;
		Number number;
		
		double p;
		boolean sameSpeaker;
		boolean firstSubj;
		

		public Entry(EntityMention ant, Context context, boolean sameSpeaker, boolean firstSubj) {
			this.head = ant.head;
			this.context = context;
			this.sameSpeaker = sameSpeaker;
			
			this.animacy = EMUtil.getAntAnimacy(ant);
			this.person = EMUtil.getAntPerson(ant.head);
			
			this.gender = EMUtil.getAntGender(ant);
			this.number = EMUtil.getAntNumber(ant);
			this.firstSubj = firstSubj;
		}
	}
}
