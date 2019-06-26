package org.apache.ibatis.test;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

public interface BlogMapper {

    @Results(id = "userResult", value = {@Result(property = "id", column = "uid", id = true)})
    @Select("SELECT * FROM blog WHERE id = #{id}")
    Blog selectBlog(Integer id);

    @SelectProvider(type = UserSqlBuilder.class, method = "buildGetUsersByName")
    List<Blog> getUsersByName(@Param("name") String name, @Param("orderByColumn") String orderByColumn);

    @ResultMap("userResult")
    List<Blog> x();

    @MapKey("id")
    Map<Integer, Blog> xx();

    int xxx(Blog Blog);

    @Insert("insert into table3 (id, name) values(#{param1}, #{param2})")
    @Options(keyProperty = "user.userId", useGeneratedKeys = true)
    @SelectKey(statement = "call next value for TestSequence", keyProperty = "nameId", before = true, resultType = int.class)
    int xxxx(String param1, String param2);

    @Insert("insert into table2 (name) values(#{name})")
    @SelectKey(statement = "call identity()", keyProperty = "nameId", before = false, resultType = int.class)
    int xxxxx(@Param("name") String name);

    //1:1
    @Select("select * from userinfo where info_id=#{info_id}")
    @Results({@Result(id = true, column = "info_id", property = "infoId", javaType = Integer.class), @Result(column = "nickName", javaType = String.class, property = "nickName"), @Result(column = "user_id", property = "user", one = @One(select = "org.apache.ibatis.test.BlogMapper.selectBlog"))})
    public Blog xxxxxx(@Param("info_id") int id);

    @Select("select * from users where user_id=#{id}")
    @Results({@Result(id = true, property = "userId", column = "user_id", javaType = Integer.class), @Result(property = "userName", column = "user_name", javaType = String.class), @Result(property = "info", column = "user_id", many = @Many(select = "org.apache.ibatis.test.BlogMapper.selectBlog"))})
    public Blog xxxxxxx(@Param("id") int id);

}
