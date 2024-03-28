package com.cfnozzle.model;

import java.util.ArrayList;
import java.util.List;

public class EventDetails {
	public ArrayList<Datum> data;
	private String subscriberId;
	private String apps;
	private String spaces;
	private String orgs;

	private List<String> appList;
	private List<String> spaceList;
	private List<String> orgsList;

	public ArrayList<Datum> getData() {
		return data;
	}

	public void setData(ArrayList<Datum> data) {
		this.data = data;
	}

	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public String getApps() {
		return apps;
	}

	public void setApps(String apps) {
		this.apps = apps;
	}

	public String getSpaces() {
		return spaces;
	}

	public void setSpaces(String spaces) {
		this.spaces = spaces;
	}

	public String getOrgs() {
		return orgs;
	}

	public void setOrgs(String orgs) {
		this.orgs = orgs;
	}

	public List<String> getAppList() {
		return appList;
	}

	public void setAppList(List<String> appList) {
		this.appList = appList;
	}

	public List<String> getSpaceList() {
		return spaceList;
	}

	public void setSpaceList(List<String> spaceList) {
		this.spaceList = spaceList;
	}

	public List<String> getOrgsList() {
		return orgsList;
	}

	public void setOrgsList(List<String> orgsList) {
		this.orgsList = orgsList;
	}

	@Override
	public String toString() {
		return "EventDetails [data=" + data + ", subscriberId=" + subscriberId + ", apps=" + apps + ", spaces=" + spaces
				+ ", orgs=" + orgs + ", appList=" + appList + ", spaceList=" + spaceList + ", orgsList=" + orgsList
				+ "]";
	}

}
