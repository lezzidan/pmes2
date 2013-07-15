
package worker.mergesort;


public class MergeSortImpl {

	public static void split(String f, String fl, String fr) throws MergeSortAppException {
		Sequence s = new Sequence(f);
		
		int leftLength = s.getLength() / 2;
		int rightLength = s.getLength() - leftLength;
		
		int[] asl = new int[leftLength];
		int[] asr = new int[rightLength];
		
		System.arraycopy(s.getSequence(), 0, 		  asl, 0, leftLength);
		System.arraycopy(s.getSequence(), leftLength, asr, 0, rightLength);
		
		Sequence sl = new Sequence(leftLength, asl);
		Sequence sr = new Sequence(rightLength, asr);
		
		sl.sequenceToDisk(fl);
		sr.sequenceToDisk(fr);
	}

	
	public static void merge(String fl, String fr, String f) throws MergeSortAppException {	
		Sequence sl = new Sequence(fl);
		Sequence sr = new Sequence(fr);
		int leftLength = sl.getLength();
		int rightLength = sr.getLength();
		Sequence s = new Sequence(leftLength + rightLength);
		
		int il = 0, ir = 0;
		while (il < leftLength && ir < rightLength) {
			if (sl.getElement(il) < sr.getElement(ir))
				s.addElement(sl.getElement(il++));
			else
				s.addElement(sr.getElement(ir++));
		}
		
		while (il < leftLength)
			s.addElement(sl.getElement(il++));
		

		while (ir < rightLength)
			s.addElement(sr.getElement(ir++));
		
		s.sequenceToDisk(f);
	}
	
}
