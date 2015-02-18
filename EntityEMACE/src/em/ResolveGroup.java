package em;

import java.io.Serializable;
import java.util.ArrayList;

import model.EntityMention;
import model.CoNLL.CoNLLPart;
import model.EntityMention.Animacy;
import model.EntityMention.Gender;
import model.EntityMention.Grammatic;
import model.EntityMention.MentionType;
import model.EntityMention.Person;
import model.EntityMention.PersonEng;
import model.EntityMention.Number;

public class ResolveGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

//	String pronoun;
	
	String anaphor;
	String head;
	ArrayList<Entry> entries;
	Animacy animacy;
	Gender gender;
	Number number;
	Grammatic gram;
	String sem = "unknown";
	String cilin = "null";
	ArrayList<EntityMention> ants;
	String anaphorName;
	CoNLLPart part;
	EntityMention m;
	
	public ResolveGroup(EntityMention m, CoNLLPart part, ArrayList<EntityMention> ants) {
		this.part = part;
		this.ants = ants;
		this.m = m;
		this.head = m.head;
		this.anaphor = m.extent;
		this.entries = new ArrayList<Entry>();
		
		this.animacy = EMUtil.getAntAnimacy(m);
		this.gender = EMUtil.getAntGender(m);
		this.number = EMUtil.getAntNumber(m);
		this.sem = EMUtil.getSemantic(m);
		this.gram = m.gram;
		this.cilin = EMUtil.getCilin(m);
		this.anaphorName = part.getPartName() + ":" + m.toName();
	}

	public static class Entry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Context context;
		String head;
		
		
		String antName;
		
		Animacy animacy = Animacy.fake;
		Gender gender = Gender.fake;
		Number number = Number.fake;
		String sem = "unknown";
		Grammatic gram;
		
		public static double p_fake_decay = 1;
		
		String cilin = "null";
		
		boolean isFake = false;
		double p;
		EntityMention ant;
		double p_c;
		int seq;
		
		public Entry(EntityMention ant, Context context, CoNLLPart part) {
			this.ant = ant;
			if(ant.isFake) {
				this.antName = "fake";
			} else {
				this.antName = part.getPartName() + ":" + ant.toName();
			}
			
			this.head = ant.head;
			this.context = context;
			this.isFake = ant.isFake;
			if(!ant.isFake) {
				this.animacy = EMUtil.getAntAnimacy(ant);
				this.gender = EMUtil.getAntGender(ant);
				this.number = EMUtil.getAntNumber(ant);
				this.sem = EMUtil.getSemantic(ant);
			}
			this.gram = ant.gram;
			this.cilin = EMUtil.getCilin(ant);
		}
	}
}
