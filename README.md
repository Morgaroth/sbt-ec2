[![Stories in In progress](https://badge.waffle.io/morgaroth/sbt-ec2.png?label=In%20Progress&title=In%20Progress)](https://waffle.io/morgaroth/sbt-ec2)  [![Stories in Ready to release](https://badge.waffle.io/morgaroth/sbt-ec2.png?label=Ready%20to%20release&title=Ready%20to%20release)](https://waffle.io/morgaroth/sbt-ec2)

# sbt-ec2

Sbt plugin for managing running instances on Amazon EC2

## Setup plugin

1 Create credentials file, e.g. in `$HOME/.ec2Credentials`

```
realm=Amazon EC2
host=<AWS Region>
user=<Access Key ID>
password=<Secret Access Key>
```

2 Add to Your project/plugins.sbt

```
addSbtPlugin("io.github.morgaroth" % "sbt-ec2" % "0.1.0")
```

3 Add EC2Plugin (as autoplugin) to project definition:

```
// enable plugin
enablePlugins(EC2Plugin)

// define to which region plugin should connect
// must be the same as specified in credentials (.ec2Credentials) file as 'host' field
ec2Region := "eu-west-1"

// add previously created credentials file
credentials += Credentials(Path.userHome / ".ec2Credentials")
```

## Documentation
### Getting List of Running Instances

You can get list of running instances using one of tasks:

* ec2FindByName &lt;name&gt;
* ec2FindByNames &lt;name&gt; &lt;name&gt; ...
* ec2GetInstances &lt;name&gt; &lt;name&gt; ...

The difference between ec2Find&#42; and ec2Get&#42; is:

* ec2Find&#42; simply finds and returns list of instances using week match of instance's name and provided query, according to the formula:
```
(query.lower contains name.lower) || (name.lower contains query.lower)
```
so return possibly wide range of results, useful in manual executing

* ec2Get&#42; finds instances by names but returns instances list which is filtered using some filtering strategy, helpful when You want to find some insances in restricted way, for this You can use:
   * one of predefined strategy from  (all of equalities in predefined strategies are case sensitive):
    * **NameEqualStrategy** - defaut, returns only instances with names exacly equal provided query names
    ```scala
     // since instance name doesn't have to be unique, You can have more instances with the same name
     instances in EC2: my-simple-server, my-simple-server, simple-server, server
     ec2GettingStategy := NameEqualStrategy
     ec2GetInstances simple-server my-simple-server /==> Some(List(simple-server,my-simple-server,my-simple-server))
     ```
    * **NameEqualForceOnlyOneStrategy** - like before one, but fails when is more than one position in result
    ```scala
    // since instance name doesn't have to be unique, You can have more instances with the same name
    // instances in EC2: my-simple-server, my-simple-server, simple-server, server
    ec2GettingStategy := NameEqualStrategyForceOnlyOne
    ec2GetInstances simple-server /==> Some(List(simple-server))
    ec2GetInstances my-simple-server /==> None
    ```
    * **InstanceNameContainNameQueryStrategy** - returns only that instances which name contains provided query
    ```scala
    // instances in EC2: my-simple-server, simple-server, server
    ec2GettingStategy := InstanceNameContainNameQuery
    ec2GetInstances simple-server /==> Some(List(my-simple-server, simple-server))
    ec2GetInstances server /==> Some(List(my-simple-server, simple-server, server))
    ```
    This may be helpful when part of name describe instances You want to get
    * **InstanceNameContainNameQueryForceOnlyOneStrategy** - like before one, but fails when is more than one position in result
    ```scala
    // instances in EC2: my-develop-server-, my-production-server, simple-server, server
    ec2GettingStategy := InstanceNameContainNameQueryForceOnlyOne
    ec2GetInstances my-develop /==> Some(List(simple-server))
    ec2GetInstances my /==> None
    ec2GetInstances server /==> None
    ```
    This may be helpful when part of name describe uniqally an instance You want to get

  * or define Your own strategy by extending trait
  ```scala
  package io.github.morgaroth.sbt.ec2
  trait GettingInstanceStrategy {
      def getInstances(queryNames: List[String], foundInstances: List[Instance]): Option[List[Instance]]
  }
   ```

