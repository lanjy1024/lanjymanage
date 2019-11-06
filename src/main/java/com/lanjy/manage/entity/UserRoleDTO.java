package com.lanjy.manage.entity;

public class UserRoleDTO {
	private Integer id;

	private String username;//用户名

	private String mobile;//手机号

	private String email;//邮箱

	private String password;//密码

	private Integer insertUid;//添加该用户的用户id

	private String insertTime;//注册时间

	private String updateTime;//修改时间

	private boolean isDel;//是否删除（0：正常；1：已删）

	private boolean isJob;//是否在职（0：正常；1，离职）

	private String roleNames;//权限名

	private Integer version;//更新版本

	private String lastLoginTime;

	public String getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(String lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username == null ? null : username.trim();
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile == null ? null : mobile.trim();
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email == null ? null : email.trim();
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password == null ? null : password.trim();
	}

	public Integer getInsertUid() {
		return insertUid;
	}

	public void setInsertUid(Integer insertUid) {
		this.insertUid = insertUid;
	}

	public String getInsertTime() {
		return insertTime == null ? "" : insertTime.substring(0,
				insertTime.length() - 2);
	}

	public void setInsertTime(String insertTime) {
		this.insertTime = insertTime;
	}

	public String getUpdateTime() {
		return updateTime == null ? "" : updateTime.substring(0,
				updateTime.length() - 2);
	}

	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	public boolean getDel() {
		return isDel;
	}

	public void setDel(boolean del) {
		isDel = del;
	}

	public boolean getJob() {
		return isJob;
	}

	public void setJob(boolean job) {
		isJob = job;
	}

	public String getRoleNames() {
		return roleNames;
	}

	public void setRoleNames(String roleNames) {
		this.roleNames = roleNames;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@Override
	public String toString() {
		return "UserRoleDTO [id=" + id + ", username=" + username + ", mobile="
				+ mobile + ", email=" + email + ", password=" + password
				+ ", insertUid=" + insertUid + ", insertTime=" + insertTime
				+ ", updateTime=" + updateTime + ", isDel=" + isDel
				+ ", isJob=" + isJob + ", roleNames=" + roleNames
				+ ", version=" + version + "]";
	}

}