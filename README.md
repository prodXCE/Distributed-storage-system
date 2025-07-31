````markdown
# Distributed File Storage System (A Mini Google Drive)

A fault-tolerant, scalable, and distributed file storage system built from the ground up using Java (Spring Boot) and FreeBSD. This project demonstrates core principles of distributed systems by emulating the architecture of modern cloud storage services.

![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green?logo=spring) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql) ![FreeBSD](https://img.shields.io/badge/FreeBSD-14-red?logo=freebsd) ![Maven](https://img.shields.io/badge/Maven-4-red?logo=apachemaven) ![KVM](https://img.shields.io/badge/KVM-Virtualization-purple?logo=linux)

---

## Overview

This project tackles the fundamental challenges of single-server storage by implementing a distributed architecture that provides:

-   **Fault Tolerance:** Files are protected against server failure by splitting them into chunks and storing multiple copies (**replication**) across independent storage nodes.
-   **Scalability:** The storage capacity of the system can be easily expanded by adding new storage nodes to the cluster (**horizontal scaling**).
-   **Centralized Metadata:** A dedicated metadata server tracks the location of every file chunk, allowing for efficient retrieval and management.

The system is built with a Java/Spring Boot backend for the API and metadata layer, and it leverages the power of FreeBSD Jails and KVM for creating a realistic, network-isolated cluster of storage nodes.

---

## System Architecture

The architecture is composed of three main components that communicate over the network:

1.  **API Gateway & Controller (Spring Boot):** The single entry point for all client requests. It handles file chunking, orchestrates storage operations, and communicates with the Metadata Server.
2.  **Metadata Server (PostgreSQL):** The "brain" of the system. It stores all information *about* the files, including their names, sizes, and a detailed map of where each chunk is physically located.
3.  **Storage Nodes (FreeBSD Jails):** Simple, lightweight services running in isolated FreeBSD Jails. Their only job is to store and serve raw data chunks on command.

```plaintext
+----------------+      (User Request)      +--------------------------------+
|     Client     | -----------------------> |    API Gateway & Controller    |
| (Browser/CLI)  |      (File Stream)       |    (Java / Spring Boot App)    |
+----------------+ <----------------------- +-----------------+--------------+
                                                             | ▲
                                          (2. Metadata Ops)  | | (3. Chunk Locations)
                                                             ▼ |
                                               +-----------------+--------------+
                                               |       Metadata Server        |
                                               |   (PostgreSQL Database)      |
                                               +------------------------------+
                                                             |
                                   (4. Store/Retrieve Chunks over Network)
                                                             |
       +-----------------------------------------------------+-----------------------------------------------------+
       |                                                     |                                                     |
       ▼                                                     ▼                                                     ▼
+----------------+                                    +----------------+                                    +----------------+
| Storage Node 1 |                                    | Storage Node 2 |                                    | Storage Node 3 |
| (FreeBSD Jail) |                                    | (FreeBSD Jail) |                                    | (FreeBSD Jail) |
| IP: 10.0.0.101 |                                    | IP: 10.0.0.102 |                                    | IP: 10.0.0.103 |
+----------------+                                    +----------------+                                    +----------------+
````

-----

## Technology Stack

  - **Backend:** Java 21, Spring Boot 3.x, Spring Data JPA
  - **Database:** PostgreSQL
  - **Virtualization:** KVM/QEMU with `virt-manager`
  - **Storage Node OS:** FreeBSD 14.0
  - **OS-Level Virtualization:** FreeBSD Jails
  - **Networking:** `pf` (Packet Filter) Firewall for NAT and Port Forwarding
  - **Build Tool:** Apache Maven

-----

## Setup and Installation

This project requires two main components: the Spring Boot application running on a Linux host, and the FreeBSD storage cluster running in a KVM virtual machine.

### Prerequisites

  - Linux Host with `virt-manager` and KVM installed.
  - Java 21 (or newer) and Maven installed on the Linux host.
  - PostgreSQL installed and running on the Linux host.
  - A FreeBSD installer `.iso`.

### 1\. Backend Setup (on Linux Host)

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/prodXCE/Distributed-storage-system.git)
    cd Distributed-storage_system
    ```

2.  **Create the database:**

    ```sql
    CREATE DATABASE distributed_fs;
    ```

3.  **Configure the application:**

      - Open `src/main/resources/application.properties`.
      - Update `spring.datasource.username` and `spring.datasource.password` with your PostgreSQL credentials.

4.  **Run the application:**

    ```bash
    mvn spring-boot:run
    ```

    The server will start on `http://localhost:8080`.

### 2\. Storage Node Setup (FreeBSD VM)

A detailed, step-by-step guide for setting up the FreeBSD VM from scratch is included in the **Appendix** at the end of this document. The high-level steps are:

1.  **Build the Storage Node App:**

      - Navigate to the `storage-node-app` directory in a separate terminal.
      - Build the executable JAR: `mvn clean package`.

2.  **Create and Configure the FreeBSD VM** using `virt-manager`.

3.  **Configure Jails, Firewall, and Services** inside the VM.

4.  **Deploy the Storage Node App** to the jails.

-----

## API Usage

All interaction is done via the REST API from the Linux host.

### 1\. Upload a File

Use `curl` to upload a file. The server will handle chunking and replication.

```bash
# Create a test file
echo "This file is stored across multiple servers!" > testfile.txt

# Upload it
curl -X POST -F "file=@testfile.txt" http://localhost:8080/api/files/upload
```

**Expected Response:**
`File uploaded successfully. File ID: <your-unique-file-id>`

### 2\. Download a File

Use the `File ID` from the upload response to download the file.

```bash
# Replace with the actual ID you received
FILE_ID="<your-unique-file-id>"

curl http://localhost:8080/api/files/download/$FILE_ID -o downloaded_file.txt
```

The `downloaded_file.txt` will be identical to the original.

### 3\. Testing Fault Tolerance

1.  **Upload a new file** and note its ID.
2.  **Inspect the storage directories** inside the FreeBSD VM (e.g., `/usr/jails/node1_root/data/chunks/`).
3.  **Manually delete** one of the chunk files from one of the nodes.
4.  **Download the file** using its ID. The download will succeed because the system will automatically fetch the replica of the missing chunk from another node.

-----

## Future Enhancements

  - **Phase 5: Security:** Implement user authentication and authorization using Spring Security and JWTs.
  - **Dynamic Node Registration:** Create a heartbeat mechanism for storage nodes to register themselves with the main server.
  - **Load Balancing:** Implement smarter chunk placement strategies instead of simple round-robin.
  - **Client Application:** Build a simple web UI or CLI tool for a better user experience.

-----

-----

## FreeBSD Configuration

This section provides the complete, step-by-step commands to configure the FreeBSD virtual machine and the storage node jails. All commands are to be run inside the FreeBSD VM unless specified otherwise.

### Part A: VM Creation and Networking (virt-manager on Linux Host)

1.  **Create a "Host-Only" Network:**

      - In `virt-manager`, go to `Edit -> Connection Details -> Virtual Networks`.
      - Click `+` to add a new network.
      - Name: `host-private`.
      - Network: `192.168.56.0/24`.
      - **Disable DHCP**.
      - Network Mode: **"Isolated network"**.

2.  **Create the VM:**

      - Create a new VM from the FreeBSD `.iso`.
      - On the final screen, check **"Customize configuration before install"**.

3.  **Configure VM Hardware:**

      - **NIC 1:** Keep the default (NAT network).
      - **Add Hardware -\> Network:** Set "Network source" to the `host-private` network you just created.

4.  **Install FreeBSD:**

      - During installation, configure the network interfaces:
          - `vtnet0` (NAT): Use DHCP.
          - `vtnet1` (Host-Only): **Do not** use DHCP. Set a static IP: `192.168.56.10`, Netmask: `255.255.255.0`.

### Part B: Initial FreeBSD Setup (Inside VM)

1.  **Enable Packet Forwarding:** This allows the VM to act as a router for the jails.

    ```sh
    sudo sysrc gateway_enable="YES"
    sudo sysctl net.inet.ip.forwarding=1
    ```

2.  **Create the Jails' Virtual Network:**

    ```sh
    sudo sysrc cloned_interfaces+="lo1"
    sudo sysrc ifconfig_lo1="inet 10.0.0.1/24"
    sudo service netif cloneup
    sudo service netif start lo1
    ```

### Part C: ZFS and Jail Filesystem Setup

1.  **Create ZFS Datasets:**

    ```sh
    sudo zfs create -o mountpoint=/jails zroot/jails
    sudo zfs create zroot/jails/node1
    sudo zfs create zroot/jails/node2
    sudo zfs create zroot/jails/node3
    ```

2.  **Install Base System Template:**

    ```sh
    sudo mkdir -p /usr/jails/base
    sudo fetch [https://download.freebsd.org/releases/amd64/14.0-RELEASE/base.txz](https://download.freebsd.org/releases/amd64/14.0-RELEASE/base.txz) -o /tmp/base.txz
    sudo tar -C /usr/jails/base -xvf /tmp/base.txz
    ```

3.  **Create Unique Jail Roots and Mount Points:**

    ```sh
    sudo mkdir /usr/jails/node1_root
    sudo mkdir /usr/jails/node2_root
    sudo mkdir /usr/jails/node3_root
    sudo cp -a /usr/jails/base/. /usr/jails/node1_root/
    sudo cp -a /usr/jails/base/. /usr/jails/node2_root/
    sudo cp -a /usr/jails/base/. /usr/jails/node3_root/
    sudo mkdir /usr/jails/node1_root/data
    sudo mkdir /usr/jails/node2_root/data
    sudo mkdir /usr/jails/node3_root/data
    ```

### Part D: Jail Configuration (`/etc/jail.conf`)

Create the `/etc/jail.conf` file with the following content:

```conf
# --- Global settings ---
exec.start = "/bin/sh /etc/rc";
exec.stop = "/bin/sh /etc/rc.shutdown";
exec.clean;
mount.devfs;

# --- Define our three storage node jails ---

storage_node_1 {
  path = "/usr/jails/node1_root";
  host.hostname = "node1.minidrive.local";
  ip4.addr = "lo1|10.0.0.101/24";
  allow.raw_sockets = 1;
  mount += "/jails/node1 /usr/jails/node1_root/data nullfs rw 0 0";
}

storage_node_2 {
  path = "/usr/jails/node2_root";
  host.hostname = "node2.minidrive.local";
  ip4.addr = "lo1|10.0.0.102/24";
  allow.raw_sockets = 1;
  mount += "/jails/node2 /usr/jails/node2_root/data nullfs rw 0 0";
}

storage_node_3 {
  path = "/usr/jails/node3_root";
  host.hostname = "node3.minidrive.local";
  ip4.addr = "lo1|10.0.0.103/24";
  allow.raw_sockets = 1;
  mount += "/jails/node3 /usr/jails/node3_root/data nullfs rw 0 0";
}
```

### Part E: Firewall Setup (`/etc/pf.conf`)

1.  Enable the `pf` firewall:

    ```sh
    sudo sysrc pf_enable="YES"
    ```

2.  Create the `/etc/pf.conf` file with the following content:

    ```conf
    # --- 1. Macros / Variables ---
    ext_if = "vtnet0"
    int_if = "vtnet1"
    jail_net = "10.0.0.0/24"

    # --- 2. Options ---
    set block-policy drop
    set skip on lo

    # --- 3. Translation Rules (NAT and RDR) ---
    nat on $ext_if from $jail_net to any -> ($ext_if)
    rdr on $int_if proto tcp from any to any port 8081 -> 10.0.0.101 port 8081
    rdr on $int_if proto tcp from any to any port 8082 -> 10.0.0.102 port 8081
    rdr on $int_if proto tcp from any to any port 8083 -> 10.0.0.103 port 8081

    # --- 4. Filter Rules ---
    block in all
    pass out all keep state
    pass in on $int_if proto icmp all
    pass in on $int_if proto tcp to port { 8081, 8082, 8083 }
    ```

3.  Start the firewall:

    ```sh
    sudo service pf start
    ```

### Part F: Storage Node Service Script (`rc.d`)

1.  Create the script file `/usr/local/etc/rc.d/storage_nodes` with the following content:

    ```sh
    #!/bin/sh
    #
    # PROVIDE: storage_nodes
    # REQUIRE: LOGIN jail

    . /etc/rc.subr

    name="storage_nodes"
    rcvar="storage_nodes_enable"

    load_rc_config $name
    : ${storage_nodes_jails:="storage_node_1 storage_node_2 storage_node_3"}

    start_cmd() {
        echo "Starting storage node services..."
        for jail_name in ${storage_nodes_jails}; do
            local pidfile="/var/run/${jail_name}.pid"
            echo " - Starting in jail: ${jail_name}"
            /usr/sbin/jexec ${jail_name} /usr/local/bin/java -jar /root/storage-node.jar > /tmp/${jail_name}.log 2>&1 &
            echo $! > "${pidfile}"
        done
    }

    stop_cmd() {
        echo "Stopping storage node services..."
        for jail_name in ${storage_nodes_jails}; do
            local pidfile="/var/run/${jail_name}.pid"
            if [ -f "${pidfile}" ]; then
                kill $(cat "${pidfile}") && rm -f "${pidfile}"
            fi
        done
    }

    status_cmd() {
        echo "Status of storage node services:"
        for jail_name in ${storage_nodes_jails}; do
            local pidfile="/var/run/${jail_name}.pid"
            if [ -f "${pidfile}" ] && ps -p $(cat "${pidfile}") > /dev/null; then
                echo " - Service in jail ${jail_name} is RUNNING (PID: $(cat "${pidfile}"))."
            else
                echo " - Service in jail ${jail_name} is STOPPED."
            fi
        done
    }

    run_rc_command "$1"
    ```

2.  Make the script executable:

    ```sh
    sudo chmod +x /usr/local/etc/rc.d/storage_nodes
    ```

3.  Enable the service in `/etc/rc.conf`:

    ```sh
    sudo sysrc storage_nodes_enable="YES"
    sudo sysrc storage_nodes_jails="storage_node_1 storage_node_2 storage_node_3"
    ```

### Part G: Final Deployment

1.  **Copy the `storage-node.jar`** from your Linux host to the FreeBSD VM's home directory using `scp`.

2.  **Install Java and Deploy JAR in Jails:**

    ```csh
    # This loop installs Java and copies the JAR into each jail
    foreach jail (storage_node_1 storage_node_2 storage_node_3)
      echo ">>> Deploying to $jail <<<"
      sudo jexec $jail pkg install -y openjdk17
      sudo cp ~/storage-node.jar /usr/jails/${jail}_root/root/
    end
    ```

3.  **Start the Jails and Services:**

    ```sh
    sudo service jail start
    sudo service storage_nodes start
    ```

The FreeBSD storage cluster is now fully configured and running.

```
```
