package com.cfnozzle.model;

import java.util.List;

public class Root {

	private String subscriberId;
	private String apps;
	private String spaces;
	private String orgs;

	private List<String> appList;
	private List<String> spaceList;
	private List<String> orgsList;

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

}
