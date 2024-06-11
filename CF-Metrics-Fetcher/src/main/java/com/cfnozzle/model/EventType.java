package com.cfnozzle.model;

import java.util.ArrayList;

public class EventType {
	public ArrayList<Datum> data;

	public ArrayList<Datum> getData() {
		return data;
	}

	public void setData(ArrayList<Datum> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "EventType [data=" + data + ", getData()=" + getData() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}

}
