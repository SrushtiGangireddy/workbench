package org.pmiops.workbench.api;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.inject.Provider;

import org.json.JSONObject;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Cluster;
import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ClusterController implements ClusterApiDelegate {
  // This will currently only work inside the Broad's network.
  private static final Logger log = Logger.getLogger(ClusterController.class.getName());

  private final NotebooksService notebooksService;
  private final Provider<User> userProvider;
  private final FireCloudService fireCloudService;

  private static final Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster> TO_ALL_OF_US_CLUSTER =
    new Function<org.pmiops.workbench.notebooks.model.Cluster, Cluster>() {
      @Override
      public Cluster apply(org.pmiops.workbench.notebooks.model.Cluster firecloudCluster) {
        Cluster allOfUsCluster = new Cluster();
        allOfUsCluster.setClusterName(firecloudCluster.getClusterName());
        allOfUsCluster.setClusterNamespace(firecloudCluster.getGoogleProject());
        allOfUsCluster.setStatus(firecloudCluster.getStatus());
        allOfUsCluster.setCreatedDate(firecloudCluster.getCreatedDate());
        allOfUsCluster.setDestroyedDate(firecloudCluster.getDestroyedDate());
        allOfUsCluster.setLabels(firecloudCluster.getLabels());;
        return allOfUsCluster;
      }
    };

  private org.pmiops.workbench.notebooks.model.ClusterRequest createFirecloudClusterRequest() {
    org.pmiops.workbench.notebooks.model.ClusterRequest firecloudClusterRequest = new org.pmiops.workbench.notebooks.model.ClusterRequest();
    // TODO: Use real paths and accounts.
    firecloudClusterRequest.setBucketPath("");
    firecloudClusterRequest.setServiceAccount("");
    Map<String, String> labels = new HashMap<String, String>();
    labels.put("all-of-us", "true");
    labels.put("created-by", userProvider.get().getEmail());
    firecloudClusterRequest.setLabels(labels);
    // TODO: Host our extension somewhere.
    // firecloudClusterRequest.setJupyterExtensionUri("");

    return firecloudClusterRequest;
  }

  private String convertClusterName(String workspaceId) {
      String clusterName = workspaceId + this.userProvider.get().getUserId();
      clusterName = clusterName.toLowerCase();
      return clusterName;
  }

  @Autowired
  ClusterController(NotebooksService notebooksService,
      Provider<User> userProvider,
      FireCloudService fireCloudService) {
    this.notebooksService = notebooksService;
    this.userProvider = userProvider;
    this.fireCloudService = fireCloudService;
  }

  public ResponseEntity<Cluster> createCluster(String workspaceNamespace,
      String workspaceId) {
    Cluster createdCluster;

    String clusterName = this.convertClusterName(workspaceId);
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      createdCluster = TO_ALL_OF_US_CLUSTER.apply(this.notebooksService.createCluster(workspaceNamespace, clusterName, createFirecloudClusterRequest()));
    } catch (ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok(createdCluster);
  }


  public ResponseEntity<EmptyResponse> deleteCluster(String workspaceNamespace,
      String workspaceId) {
    String clusterName = this.convertClusterName(workspaceId);
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      this.notebooksService.deleteCluster(workspaceNamespace, clusterName);
    } catch (ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    EmptyResponse e = new EmptyResponse();
    return ResponseEntity.ok(e);
  }


  public ResponseEntity<Cluster> getCluster(String workspaceNamespace,
      String workspaceId) {
    String clusterName = this.convertClusterName(workspaceId);
    Cluster cluster;
    try {
      // TODO: Replace with real workspaceNamespace/billing-project
      cluster = TO_ALL_OF_US_CLUSTER.apply(this.notebooksService.getCluster(workspaceNamespace, clusterName));
    } catch(ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    return ResponseEntity.ok(cluster);
  }


  public ResponseEntity<ClusterListResponse> listClusters(String labels) {
    List<Cluster> newClusters = new ArrayList<Cluster>();
    List<org.pmiops.workbench.notebooks.model.Cluster> oldClusters;
    try {
      oldClusters = this.notebooksService.listClusters(labels);
    } catch(ApiException e) {
      // TODO: Actually handle errors reasonably
      throw new RuntimeException(e);
    }
    ClusterListResponse response = new ClusterListResponse();
    response.setItems(oldClusters.stream().map(TO_ALL_OF_US_CLUSTER).collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Void> localizeNotebook(String workspaceNamespace, String workspaceId, List<FileDetail> fileList) {
    try{
      String clusterName = convertClusterName(workspaceId);
      this.notebooksService.localize(workspaceNamespace, clusterName, convertfileDetailsToJson(workspaceNamespace, workspaceId, fileList));
      } catch (ApiException e) {
        throw new RuntimeException(e);
      }catch(Exception e) {
        throw new RuntimeException(e);
    }
    return null;
  }

  /**
   * Create a map with key as ~/workspaceName/workspaceID/fileName and value as the gs://firecloudBucket name/filename
   * @param workspaceNamespace
   * @param workspaceId
   * @param fileList
   * @return
   * @throws org.pmiops.workbench.firecloud.ApiException
   */
  private HashMap convertfileDetailsToJson(String workspaceNamespace, String workspaceId,List<FileDetail> fileList) throws org.pmiops.workbench.firecloud.ApiException {
    HashMap fileDetailsMap = new HashMap();
    try {
      org.pmiops.workbench.firecloud.model.WorkspaceResponse workspaceResponse = fireCloudService.getWorkspace(workspaceNamespace, workspaceId);
      if (workspaceResponse != null) {
        org.pmiops.workbench.firecloud.model.Workspace fireCloudWorkspace = workspaceResponse.getWorkspace();
        if (fireCloudWorkspace != null) {
          String bucketName = fireCloudWorkspace.getBucketName();
          for (FileDetail fileDetails : fileList) {
            StringBuffer key = new StringBuffer("~/");
            key.append(workspaceNamespace);
            key.append("/");
            key.append(workspaceId).append("/").append(fileDetails.getName());

            StringBuffer value = new StringBuffer();
            value.append("gs://");
            value.append(bucketName);
            value.append("/");
            value.append(fileDetails.getName());
            fileDetailsMap.put(key.toString(), value);
          }
        }
      }
    }catch(org.pmiops.workbench.firecloud.ApiException ex)
    {
      throw ex;
    }
    return fileDetailsMap;
  }
}
