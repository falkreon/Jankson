package blue.endless.jankson.api.document.view;

import java.util.AbstractList;

import blue.endless.jankson.api.document.ArrayElement;
import blue.endless.jankson.api.document.DocumentElement;
import blue.endless.jankson.api.document.ValueElement;

/**
 * EXPERIMENTAL, NEEDS OPTIMIZING: Value-only view of the underlying document element.
 */
public class ArrayElementValueView extends AbstractList<ValueElement> {
	private ArrayElement parent;
	
	public ArrayElementValueView(ArrayElement parent) {
		this.parent = parent;
	}
	
	private int getIndex(int valueIndex) {
		int index = -1;
		for(int i=0; i<parent.size(); i++) {
			if (parent.get(i).isValueEntry()) index++;
			if (index==valueIndex) return i;
		}
		return -1;
	}
	
	@Override
	public ValueElement get(int index) {
		return (ValueElement) parent.get(getIndex(index));
	}
	
	@Override
	public ValueElement set(int index, ValueElement element) {
		DocumentElement result = parent.set(getIndex(index), element);
		return (result.isValueEntry()) ? result.asValueEntry() : null;
	}
	
	@Override
	public void add(int index, ValueElement element) {
		parent.add(getIndex(index), element);
	}
	
	@Override
	public ValueElement remove(int index) {
		return (ValueElement) parent.remove(getIndex(index));
	}
	
	@Override
	public int size() {
		int result = 0;
		for(DocumentElement elem : parent) {
			if (elem.isValueEntry()) result++;
		}
		return result;
	}
}
