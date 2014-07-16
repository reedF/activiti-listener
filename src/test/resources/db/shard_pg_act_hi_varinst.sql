-- 针对工作流变量数据增长快，对act_hi_varinst表进行分表策略:
---创建模式history
create schema history;

-- pg函数：定期执行，返回影响的记录数
-- 对已完成的流程且结束时间为1个月前的流程变量数据，
-- 按创建年份${year}和${month}提取到历史表'act_hi_varinst_${year}_${month}'
-- 删除act_hi_varinst表中相应数据（注：未实现）
create or replace function dump_act_hi_varinst() returns integer as
$body$
declare
dumpdate date := (current_date - interval '1 month');
table_index integer := cast(extract(month from dumpdate) as integer); 
t_year integer := cast(extract(year from dumpdate) as integer);    
schema_name varchar := 'history';
t_name varchar := 'act_hi_varinst_' || t_year || '_' || table_index;
table_name varchar := schema_name || '.' || t_name;
isExist integer := (select count(*) from pg_class where relname = t_name);
itemNum integer := (select count(v.id_) from act_hi_varinst v inner join act_hi_procinst p on v.proc_inst_id_ = p.proc_inst_id_
where p.end_time_ is not null and p.end_time_ < dumpdate);
begin
	if (itemNum > 0) 
	then
		if (isExist = 0)
		then 
			execute 'create table ' || table_name || ' as (
			select v.*,p.start_time_ from act_hi_varinst v inner join act_hi_procinst p on v.proc_inst_id_ = p.proc_inst_id_
			where p.end_time_ is not null and p.end_time_ < to_date(''' || dumpdate || ''',''YYYY-MM'')' ||
			');';
			execute 'ALTER TABLE ' || table_name || ' ADD PRIMARY KEY (id_);';			
		else 
			execute 'insert into ' || table_name || ' (
			select v.*,p.start_time_ from act_hi_varinst v inner join act_hi_procinst p on v.proc_inst_id_ = p.proc_inst_id_
			where p.end_time_ is not null and p.end_time_ < to_date(''' || dumpdate || ''',''YYYY-MM'')' ||
			');';
		end if;
	end if;
return itemNum;
end;
$body$
Language plpgsql;

-- 执行分表函数进行分表
select * from dump_act_hi_varinst();


