package com.sonarsource.sonarupdatecenter;

import java.util.List;

import net.bican.wordpress.FilterPost;
import net.bican.wordpress.Post;
import net.bican.wordpress.Wordpress;
import net.bican.wordpress.exceptions.InsufficientRightsException;
import net.bican.wordpress.exceptions.InvalidArgumentsException;
import net.bican.wordpress.exceptions.ObjectNotFoundException;
import redstone.xmlrpc.XmlRpcFault;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

      try {

        Wordpress wp = new Wordpress("sonar", "sonar", "http://localhost/wordpress/xmlrpc.php");
        FilterPost filter = new FilterPost();
        filter.setNumber(1);
        filter

        List<Post> recentPosts = wp.getPosts(filter);
        System.out.println("Here are the ten recent posts:");
        for (Post page : recentPosts) {
          System.out.println(page.getPost_id() + ":" + page.getPost_title());
        }

        System.out.println("Posting a test (draft) page...");
        Post recentPost = new Post();
        recentPost.setPost_title("Test Page");
        recentPost.setPost_content("Test description");
        recentPost.setPost_status("draft");
        Integer result = wp.newPost(recentPost);
        System.out.println("new post page id: " + result);


        System.out.println("That is all folks!");
      } catch( Exception e  ) {
        System.err.println("Bad world!");
        System.err.println(e.getMessage());
      }
    }
}
