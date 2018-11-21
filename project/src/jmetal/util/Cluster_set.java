package jmetal.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import jmetal.core.Solution;

/**
 *
 * @author Ana Belen
 */
public class Cluster_set{
    
    // Data members
  private Solution representative_;
  /**
   * Stores a list of <code>solution</code> objects.
   */
  protected List<Solution> solutionsList_;
  
  /** 
   * Maximum size of the solution set 
   */
  private int capacity_ = 0; 
    
  /**
   * Constructor.
   * Creates an unbounded solution set.
   */
  public Cluster_set() {
    solutionsList_ = new ArrayList<Solution>();
    representative_ = new Solution();
  } 
    
  /** 
   * Creates a empty solutionSet with a maximum capacity.
   * @param maximumSize Maximum size.
   */
  public Cluster_set(int maximumSize){    
    solutionsList_ = new ArrayList<Solution>();
    capacity_      = maximumSize;
    representative_ = new Solution();
  } 
  
  /** 
   * Returns the number of solutions in the SolutionSet.
   * @return The size of the SolutionSet.
   */  
  public int size(){
    return solutionsList_.size();
  } // size
  
  /** 
   * Empties the SolutionSet
   */
  public void clear(){
    solutionsList_.clear();
    representative_ = null;
  } // clear
  
  /** 
  * Inserts a new solution into the SolutionSet. 
  * @param solution The <code>Solution</code> to store
  * @return True If the <code>Solution</code> has been inserted, false 
  * otherwise. 
  */
  public boolean add(Solution solution) {
    if (solutionsList_.size() == capacity_) {
      Configuration.logger_.severe("The population is full");
      Configuration.logger_.severe("Capacity is : "+capacity_);
      Configuration.logger_.severe("\t Size is: "+ this.size());
      return false;
    } // if
    
    solutionsList_.add(solution);
    return true;
  } // add
    
  /**
   * Returns the ith solution in the set.
   * @param i Position of the solution to obtain.
   * @return The <code>Solution</code> at the position i.
   * @throws IndexOutOfBoundsException.
   */
  public Solution get(int i) {
    if (i >= solutionsList_.size()) {
      throw new IndexOutOfBoundsException("Index out of Bound "+i);
    }
    return solutionsList_.get(i);
  } // get
  
  public Solution getRespresentative() {
      Distance distance = new Distance();
      // List that contains the index of a solution and its average distance 
      // to the rest of solutions in the cluster:        
      LinkedList<DistanceNode> dist_list = new LinkedList<>();
      double dist;
      dist = 0;
    
      // If the cluster is formed by just one individual, this is the representative:
      if (this.size() == 1)
          this.representative_ = this.solutionsList_.get(0);
      // Otherwise, the representative is the individual having the minimum 
      // average distance from the rest of solutions in the cluster:
      else
      {
      // Calculate the average distance of each individual to the rest of individuals:
      for(int i = 0; i < this.size(); i++){
          dist = 0;
          for(int j = 0; j < this.size(); j++){
              if(!this.get(i).equals(this.get(j)))
                  dist = dist + distance.distanceBetweenObjectives(this.get(i), this.get(j));
          }
          dist = dist / (((double) this.size()) - 1);
          DistanceNode node = new DistanceNode(dist, i);
          dist_list.add(node);
      }
      // Find the individual with the minimum distance:
      dist = 1e10;
      DistanceNode node_rep = new DistanceNode(dist, 1);
      for(DistanceNode n : dist_list){
          if(n.getDistance() < dist){
              dist = n.getDistance();
              node_rep.setDistance(dist);
              node_rep.setReferece(n.getReference());
          }
      }
    
      this.representative_ = this.get(node_rep.getReference());
      }
      
      return representative_;
  }
}
