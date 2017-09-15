# coding: utf-8

"""
    AllOfUs Workbench API

    The API for the AllOfUs workbench.

    OpenAPI spec version: 0.1.0
    
    Generated by: https://github.com/swagger-api/swagger-codegen.git
"""


from pprint import pformat
from six import iteritems
import re


class Cohort(object):
    """
    NOTE: This class is auto generated by the swagger code generator program.
    Do not edit the class manually.
    """


    """
    Attributes:
      swagger_types (dict): The key is attribute name
                            and the value is attribute type.
      attribute_map (dict): The key is attribute name
                            and the value is json key in definition.
    """
    swagger_types = {
        'id': 'str',
        'name': 'str',
        'criteria': 'str',
        'type': 'str',
        'description': 'str',
        'creator': 'str',
        'creation_time': 'datetime',
        'last_modified_time': 'datetime'
    }

    attribute_map = {
        'id': 'id',
        'name': 'name',
        'criteria': 'criteria',
        'type': 'type',
        'description': 'description',
        'creator': 'creator',
        'creation_time': 'creationTime',
        'last_modified_time': 'lastModifiedTime'
    }

    def __init__(self, id=None, name=None, criteria=None, type=None, description=None, creator=None, creation_time=None, last_modified_time=None):
        """
        Cohort - a model defined in Swagger
        """

        self._id = None
        self._name = None
        self._criteria = None
        self._type = None
        self._description = None
        self._creator = None
        self._creation_time = None
        self._last_modified_time = None
        self.discriminator = None

        if id is not None:
          self.id = id
        self.name = name
        self.criteria = criteria
        self.type = type
        if description is not None:
          self.description = description
        if creator is not None:
          self.creator = creator
        if creation_time is not None:
          self.creation_time = creation_time
        if last_modified_time is not None:
          self.last_modified_time = last_modified_time

    @property
    def id(self):
        """
        Gets the id of this Cohort.

        :return: The id of this Cohort.
        :rtype: str
        """
        return self._id

    @id.setter
    def id(self, id):
        """
        Sets the id of this Cohort.

        :param id: The id of this Cohort.
        :type: str
        """

        self._id = id

    @property
    def name(self):
        """
        Gets the name of this Cohort.

        :return: The name of this Cohort.
        :rtype: str
        """
        return self._name

    @name.setter
    def name(self, name):
        """
        Sets the name of this Cohort.

        :param name: The name of this Cohort.
        :type: str
        """
        if name is None:
            raise ValueError("Invalid value for `name`, must not be `None`")

        self._name = name

    @property
    def criteria(self):
        """
        Gets the criteria of this Cohort.
        Internal representation of the cohort definition. Clients should not depend directly on this, but instead call client functions to issue a SQL query for the cohort. 

        :return: The criteria of this Cohort.
        :rtype: str
        """
        return self._criteria

    @criteria.setter
    def criteria(self, criteria):
        """
        Sets the criteria of this Cohort.
        Internal representation of the cohort definition. Clients should not depend directly on this, but instead call client functions to issue a SQL query for the cohort. 

        :param criteria: The criteria of this Cohort.
        :type: str
        """
        if criteria is None:
            raise ValueError("Invalid value for `criteria`, must not be `None`")

        self._criteria = criteria

    @property
    def type(self):
        """
        Gets the type of this Cohort.

        :return: The type of this Cohort.
        :rtype: str
        """
        return self._type

    @type.setter
    def type(self, type):
        """
        Sets the type of this Cohort.

        :param type: The type of this Cohort.
        :type: str
        """
        if type is None:
            raise ValueError("Invalid value for `type`, must not be `None`")

        self._type = type

    @property
    def description(self):
        """
        Gets the description of this Cohort.

        :return: The description of this Cohort.
        :rtype: str
        """
        return self._description

    @description.setter
    def description(self, description):
        """
        Sets the description of this Cohort.

        :param description: The description of this Cohort.
        :type: str
        """

        self._description = description

    @property
    def creator(self):
        """
        Gets the creator of this Cohort.

        :return: The creator of this Cohort.
        :rtype: str
        """
        return self._creator

    @creator.setter
    def creator(self, creator):
        """
        Sets the creator of this Cohort.

        :param creator: The creator of this Cohort.
        :type: str
        """

        self._creator = creator

    @property
    def creation_time(self):
        """
        Gets the creation_time of this Cohort.

        :return: The creation_time of this Cohort.
        :rtype: datetime
        """
        return self._creation_time

    @creation_time.setter
    def creation_time(self, creation_time):
        """
        Sets the creation_time of this Cohort.

        :param creation_time: The creation_time of this Cohort.
        :type: datetime
        """

        self._creation_time = creation_time

    @property
    def last_modified_time(self):
        """
        Gets the last_modified_time of this Cohort.

        :return: The last_modified_time of this Cohort.
        :rtype: datetime
        """
        return self._last_modified_time

    @last_modified_time.setter
    def last_modified_time(self, last_modified_time):
        """
        Sets the last_modified_time of this Cohort.

        :param last_modified_time: The last_modified_time of this Cohort.
        :type: datetime
        """

        self._last_modified_time = last_modified_time

    def to_dict(self):
        """
        Returns the model properties as a dict
        """
        result = {}

        for attr, _ in iteritems(self.swagger_types):
            value = getattr(self, attr)
            if isinstance(value, list):
                result[attr] = list(map(
                    lambda x: x.to_dict() if hasattr(x, "to_dict") else x,
                    value
                ))
            elif hasattr(value, "to_dict"):
                result[attr] = value.to_dict()
            elif isinstance(value, dict):
                result[attr] = dict(map(
                    lambda item: (item[0], item[1].to_dict())
                    if hasattr(item[1], "to_dict") else item,
                    value.items()
                ))
            else:
                result[attr] = value

        return result

    def to_str(self):
        """
        Returns the string representation of the model
        """
        return pformat(self.to_dict())

    def __repr__(self):
        """
        For `print` and `pprint`
        """
        return self.to_str()

    def __eq__(self, other):
        """
        Returns true if both objects are equal
        """
        if not isinstance(other, Cohort):
            return False

        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        """
        Returns true if both objects are not equal
        """
        return not self == other
